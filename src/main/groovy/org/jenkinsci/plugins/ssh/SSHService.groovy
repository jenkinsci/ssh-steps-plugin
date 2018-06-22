package org.jenkinsci.plugins.ssh

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings
import groovy.util.logging.Slf4j
import org.hidetake.groovy.ssh.Ssh
import org.hidetake.groovy.ssh.connection.AllowAnyHosts
import org.hidetake.groovy.ssh.core.Service
import org.hidetake.groovy.ssh.core.settings.LoggingMethod
import org.jenkinsci.plugins.ssh.util.Common
import org.jenkinsci.plugins.ssh.util.CustomLogHandler
import org.slf4j.MDC

import java.util.logging.Logger

/**
 * SSH Service, wrapper on top of hidetake's ssh service.
 *
 * @author Naresh Rayapati
 */
@Slf4j
@SuppressFBWarnings
class SSHService implements Serializable {

    private Map remote
    private boolean failOnError = true
    private boolean dryRunFlag = false
    private transient PrintStream logger
    private transient Service ssh

    /**
     * Constructor.
     *
     * @param remote
     * @param failOnError
     * @param dryRun
     * @param logger
     */
    private SSHService(
            final Map remote,
            final boolean failOnError, final boolean dryRun, final PrintStream logger) {
        this.remote = remote
        this.logger = logger
        this.failOnError = failOnError
        this.dryRunFlag = dryRun
        validateRemote()
        ssh = Ssh.newService();
    }

    static SSHService create(final Map remote,
                             final boolean failOnError,
                             final boolean dryRun, final PrintStream logger) {
        new SSHService(remote, failOnError, dryRun, logger);
    }

    /**
     * Register Log handler for all hidetake's classes.
     */
    private void registerLogHandler() {
        Logger.getLogger("org.hidetake").addHandler(new CustomLogHandler(logger, MDC.get("execution.id")))
    }

    private void validateRemote() {
        new Common(logger).validateRemote(this.remote);
    }

    private void defineRemote() {
        registerLogHandler()
        ssh.remotes {
            "$remote.name" {
                host = remote.host
                if (remote.port)
                    port = remote.port
            }
        }
        defineProxy()
    }

    private void defineProxy() {
        def proxy = remote.proxy
        if (proxy) {
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
    }

    // Settings defined globally.
    def defineSettings = {
        user = remote.user
        if (remote.password)
            password = remote.password

        // Gateway and proxy.
        if (remote.gateway)
            gateway = remote.gateway

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

        // Pipe logs to TaskListener's print stream.
        interaction = {
            when(line: _, from: standardOutput) {
                logger.println("$remote.name|$it")
            }
            when(line: _, from: standardError) {
                logger.println("$remote.name|$it")
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
    }

    /**
     * Executes given command with sudo (optional).
     *
     * @param command shell command.
     * @param sudo execute it as sudo when true.
     * @return response from ssh run.
     */
    def executeCommand(String command, boolean sudo) {
        defineRemote()
        ssh.run {
            settings {
                defineSettings.delegate = delegate
                defineSettings()
                if (remote.proxy)
                    proxy = ssh.proxies."$remote.proxy.name"
            }
            session(ssh.remotes."$remote.name") {
                if (sudo)
                    executeSudo command, pty: true
                else
                    execute command, pty: true
            }
        }
    }

    /**
     * Executes a given script.
     *
     * @param pathname file name from workspace.
     * @return response from ssh run.
     */
    def executeScriptFromFile(String pathname) {
        defineRemote()
        ssh.run {
            settings {
                defineSettings.delegate = delegate
                defineSettings()
                if (remote.proxy)
                    proxy = ssh.proxies."$remote.proxy.name"
            }
            session(ssh.remotes."$remote.name") {
                executeScript new File(pathname)
            }
        }
    }

    /**
     * Puts a file to remote node.
     *
     * @param from location to put file to.
     * @param into location to put file from.
     * @return response from ssh run.
     */
    def put(String from, String into) {
        defineRemote()

        ssh.run {
            settings {
                defineSettings.delegate = delegate
                defineSettings()
                if (remote.proxy)
                    proxy = ssh.proxies."$remote.proxy.name"
            }
            session(ssh.remotes."$remote.name") {
                put from: from, into: into
            }
        }
    }

    /**
     * Get a file from remote.
     *
     * @param from location to get file from.
     * @param into location to get file into.
     * @return
     */
    def get(String from, String into) {
        defineRemote()

        ssh.run {
            settings {
                defineSettings.delegate = delegate
                defineSettings()
                if (remote.proxy)
                    proxy = ssh.proxies."$remote.proxy.name"
            }
            session(ssh.remotes."$remote.name") {
                get from: from, into: into
            }
        }
    }

    /**
     * Remove's a file from remote node.
     *
     * @param name name of the file/dir.
     * @return output from ssh's remove operation.
     */
    def remove(String path) {
        defineRemote()
        ssh.run {
            settings {
                defineSettings.delegate = delegate
                defineSettings()
                if (remote.proxy)
                    proxy = ssh.proxies."$remote.proxy.name"
            }
            session(ssh.remotes."$remote.name") {
                remove path
            }
        }
    }
}