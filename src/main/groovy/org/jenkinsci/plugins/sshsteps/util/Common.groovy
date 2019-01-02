package org.jenkinsci.plugins.sshsteps.util

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings
import org.hidetake.groovy.ssh.core.ProxyType

import java.util.logging.Level

/**
 * Basic validation for remote.
 *
 * @author Naresh Rayapati
 *
 */
@SuppressFBWarnings
class Common {

    private final PrintStream logger

    Common(PrintStream logger) {
        this.logger = logger
    }

    static String getPrefix() {
        "SSH Steps: " as String
    }

    /**
     * Validate remote.
     *
     * @param remote
     */
    void validateRemote(remote) {
        assert remote, getPrefix() + "remote is null or empty"
        assert remote.name, getPrefix() + " a remote (or a gateway) is missing the required field 'name'"
        if (remote.retryCount)
            assert remote.retryCount >= 0, getPrefix() + "retryCount must be zero or positive ($remote.name)"
        if (remote.retryWaitSec)
            assert remote.retryWaitSec >= 0, getPrefix() + "retryWaitSec must be zero or positive ($remote.name)"
        if (remote.keepAliveSec)
            assert remote.keepAliveSec >= 0, getPrefix() + "keepAliveSec must be zero or positive ($remote.name)"
        validateUserAuthentication(remote)
        validateHostAuthentication(remote)
        validateProxyConnection(remote)
        if (remote.logLevel) {
            validateLogLevel(remote)
        }
        if (remote.gateway) {
            validateRemote(remote.gateway)
        }
    }

    /**
     * Validates user authentication.
     *
     * @param remote map of settings.
     */
    private void validateUserAuthentication(remote) {
        assert remote.user, getPrefix() + "user must be given ($remote.name)"
    }

    /**
     * Validate host params from the given remote.
     *
     * @param remote map of settings.
     */
    private void validateHostAuthentication(remote) {
        if (remote.knownHosts) {
            remote.knownHosts = new File("$remote.knownHosts")
        }
        if (!remote.allowAnyHosts && !remote.knownHosts) {
            throw new IllegalArgumentException(getPrefix() + "knownHosts must be provided when allowAnyHosts is false: $remote.name")
        }

        if (remote.identity) {
            remote.identity = remote.identity
        } else if (remote.identityFile) {
            remote.identity = new File("$remote.identityFile")
            remote.remove('identityFile')
        }
    }

    /**
     * Validate proxy arguments from remote.
     *
     * @param remote map of values.
     */
    private void validateProxyConnection(remote) {
        def proxy = remote.proxy
        if (proxy) {
            assert proxy.name, getPrefix() + " proxy name must be given ($remote.name)"
            if (!ProxyType.values().contains(ProxyType.valueOf(proxy.type))) {
                throw new IllegalArgumentException(getPrefix() + "Unsupported ProxyType ${proxy.type}. Supported types: ${ProxyType.collect { "$it" }.join(', ')}.")
            }
            if (!proxy.user && proxy.password) {
                logger.println(getPrefix() + "proxy.password is set but proxy.user is null. Credentials are ignored for proxy '${proxy.name}'")
            }
        }
    }

    /**
     * Validate log level.
     *
     * @param remote map of values.
     */
    private void validateLogLevel(remote) {
        try {
            Level.parse(remote.logLevel)
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException(getPrefix() + "Bad log level $remote.logLevel for $remote.name")
        }
    }
}
