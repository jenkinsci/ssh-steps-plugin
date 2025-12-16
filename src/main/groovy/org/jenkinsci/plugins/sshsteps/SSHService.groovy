package org.jenkinsci.plugins.sshsteps

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings
import groovy.util.logging.Slf4j
import org.hidetake.groovy.ssh.Ssh
import org.hidetake.groovy.ssh.connection.AllowAnyHosts
import org.hidetake.groovy.ssh.core.Service
import org.hidetake.groovy.ssh.core.settings.LoggingMethod
import org.jenkinsci.plugins.sshsteps.util.Common
import org.jenkinsci.plugins.sshsteps.util.CustomLogHandler
import org.slf4j.MDC

import java.util.logging.Level
import java.util.logging.Logger

/**
 * SSH Service, wrapper on top of hidetake's ssh service.
 *
 * @author Naresh Rayapati
 */
@Slf4j
@SuppressFBWarnings
class SSHService implements Serializable {

    private final Map remote
    private final boolean failOnError
    private final boolean dryRunFlag
    private final transient PrintStream logger
    private final transient Service ssh

    /**
     * Constructor.
     *
     * @param remote
     * @param failOnError
     * @param dryRun
     * @param logger
     */
    private SSHService(Map remote, boolean failOnError, boolean dryRun, PrintStream logger) {
        this.remote = remote
        this.logger = logger
        this.failOnError = failOnError
        this.dryRunFlag = dryRun
        validateRemote()
        ssh = Ssh.newService()
    }

    static SSHService create(Map remote, boolean failOnError, boolean dryRun, PrintStream logger) {
        new SSHService(remote, failOnError, dryRun, logger)
    }

    private transient CustomLogHandler currentHandler
    
    /**
     * Register Log handler for all hidetake's classes.
     */
    private void registerLogHandler(message) {
        Logger rootLogger = Logger.getLogger("org.hidetake")
        
        // Add new handler with buffering configuration from remote settings
        def bufferSize = remote.logBufferSize ?: 50
        def flushIntervalMs = remote.logFlushIntervalMs ?: 100
        def rateLimitLinesPerSec = remote.logRateLimitLinesPerSec ?: 1000
        
        currentHandler = new CustomLogHandler(logger, MDC.get("execution.id"), 
                                               bufferSize, flushIntervalMs, rateLimitLinesPerSec)
        rootLogger.addHandler(currentHandler)
        
        if (remote.logLevel) {
            rootLogger.setLevel(Level.parse(remote.logLevel))
        } else {
            logger.println(message)
            rootLogger.setLevel(Level.SEVERE)
        }
    }
    
    /**
     * Clean up the log handler for this service instance.
     * Called when the SSH operation completes.
     */
    private void cleanupLogHandler() {
        if (currentHandler != null) {
            try {
                Logger rootLogger = Logger.getLogger("org.hidetake")
                rootLogger.removeHandler(currentHandler)
                currentHandler.close()
            } catch (Exception e) {
                // Ignore cleanup errors
                log.debug("Error cleaning up log handler", e)
            } finally {
                currentHandler = null
            }
        }
    }

    private void validateRemote() {
        new Common(logger).validateRemote(this.remote)
    }

    private void defineRemote(remote, boolean enableInteraction = true) {
        ssh.remotes {
            "$remote.name" {
                host = remote.host
                if (remote.port)
                    port = remote.port
                user = remote.user
                if (remote.password)
                    password = remote.password

                // Gateway.
                if (remote.gateway) {
                    defineRemote(remote.gateway, enableInteraction)
                    gateway = ssh.remotes."$remote.gateway.name"
                }

                // Connection Settings applicable for Command, Script, FTP/SCP Operations.
                timeoutSec = remote.timeoutSec
                retryCount = remote.retryCount
                agent = remote.agent
                dryRun = dryRunFlag
                retryWaitSec = remote.retryWaitSec
                if (remote.keepAliveSec)
                    keepAliveSec = remote.keepAliveSec
                jschLog = true

                // Agent forwarding for command, need to find the difference between this and agent.
                if (remote.agentForwarding)
                    agentForwarding = remote.agentForwarding

                // Ignore error don't fail the pipeline build
                ignoreError = !failOnError

                if (remote.fileTransfer)
                    fileTransfer = remote.fileTransfer

                // Avoid excessive logging in Jenkins master.
                logging = LoggingMethod.none

                def logPrefix = remote.appendName ? "$remote.name|" : ''

                // Pipe logs to TaskListener's print stream for commands/scripts only
                // Do NOT enable interaction for file transfers to prevent file contents from being printed
                if (enableInteraction) {
                    interaction = {
                        when(line: _, from: standardOutput) {
                            logger.println("$logPrefix$it")
                        }
                        when(line: _, from: standardError) {
                            logger.println("$logPrefix$it")
                        }
                    }
                }

                if (remote.pty) {
                    pty = remote.pty
                }

                // Encoding
                if (remote.encoding)
                    encoding = remote.encoding

                // Host authentication
                if (remote.allowAnyHosts)
                    knownHosts = AllowAnyHosts.instance
                else if (remote.knownHosts)
                    knownHosts = remote.knownHosts

                // Public and private key authentication
                if (remote.identity)
                    identity = remote.identity
                passphrase = remote.passphrase

                // Proxy.
                if (remote.proxy) {
                    defineProxy(remote.proxy)
                    proxy = ssh.proxies."$remote.proxy.name"
                }

            }
        }
    }

    private void defineProxy(proxy) {
        ssh.proxies {
            "$proxy.name" {
                host = proxy.host
                type = proxy.type
                if (proxy.port)
                    port = proxy.port
                if (proxy.socksVersion)
                    socksVersion = proxy.socksVersion
                if (proxy.user)
                    user = proxy.user
                if (proxy.password)
                    password = proxy.password
            }
        }
    }

    /**
     * Executes given command with sudo (optional).
     *
     * @param command shell command.
     * @param sudo execute it as sudo when true.
     * @return response from ssh run.
     */
    def executeCommand(String command, boolean sudo) {
        try {
            registerLogHandler("Executing command on $remote.name[$remote.host]: $command sudo: $sudo")
            defineRemote(remote)
            ssh.run {
                session(ssh.remotes."$remote.name") {
                    if (sudo)
                        executeSudo command
                    else
                        execute command
                }
            }
        } finally {
            cleanupLogHandler()
        }
    }

    /**
     * Executes a given script.
     *
     * @param pathname file name from workspace.
     * @return response from ssh run.
     */
    def executeScriptFromFile(String pathname) {
        try {
            registerLogHandler("Executing script on $remote.name[$remote.host]: $pathname")
            defineRemote(remote)
            ssh.run {
                session(ssh.remotes."$remote.name") {
                    executeScript new File(pathname)
                }
            }
        } finally {
            cleanupLogHandler()
        }
    }

    /**
     * Puts a file to remote node.
     *
     * @param from location to put file to.
     * @param into location to put file from.
     * @param filterBy put files by a file filter.
     * @param filterRegex filter regex.
     * @return response from ssh run.
     */
    def put(String from, String into, String filterBy, String filterRegex) {
        try {
            registerLogHandler("Sending a file/directory to $remote.name[$remote.host]: from: $from into: $into")
            // Disable interaction for file transfers to prevent file contents from being printed
            defineRemote(remote, false)
            ssh.run {
                session(ssh.remotes."$remote.name") {
                    if (filterBy && filterRegex)
                        put from: from, into: into, filter: { it."$filterBy" =~ filterRegex }
                    else
                        put from: from, into: into
                }
            }
        } finally {
            cleanupLogHandler()
        }
    }

    /**
     * Gets a file from remote node.
     *
     * @param from location to get file from.
     * @param into location to get file into.
     * @param filterBy get files by a file filter.
     * @param filterRegex filter regex.
     * @return response from ssh run.
     */
    def get(String from, String into, String filterBy, String filterRegex) {
        try {
            registerLogHandler("Receiving a file/directory from $remote.name[$remote.host]: from: $from into: $into")
            // Disable interaction for file transfers to prevent file contents from being printed
            defineRemote(remote, false)
            ssh.run {
                session(ssh.remotes."$remote.name") {
                    if (filterBy && filterRegex)
                        get from: from, into: into, filter: { it."$filterBy" =~ filterRegex }
                    else
                        get from: from, into: into
                }
            }
        } finally {
            cleanupLogHandler()
        }
    }

    /**
     * Removes a file from remote node.
     *
     * @param name name of the file/dir.
     * @return output from ssh's remove operation.
     */
    def remove(String path) {
        try {
            registerLogHandler("Removing a file/directory on $remote.name[$remote.host]: $path")
            defineRemote(remote)
            ssh.run {
                session(ssh.remotes."$remote.name") {
                    remove path
                }
            }
        } finally {
            cleanupLogHandler()
        }
    }
}
