package io.github.ktor_batterypack.database

import com.zaxxer.hikari.HikariCredentialsProvider
import com.zaxxer.hikari.util.Credentials
import java.net.URI
import software.amazon.awssdk.auth.credentials.AwsCredentialsProvider
import software.amazon.awssdk.regions.Region
import software.amazon.awssdk.services.rds.RdsUtilities
import software.amazon.awssdk.services.rds.model.GenerateAuthenticationTokenRequest

/**
 * [HikariCredentialsProvider] that authenticates to AWS RDS using IAM database authentication.
 *
 * Instead of a static password, generates a short-lived IAM auth token (valid for 15 minutes)
 * via [RdsUtilities.generateAuthenticationToken] and returns it as the password. The token is
 * regenerated for every new connection HikariCP creates, so no token caching or refresh
 * scheduling is needed.
 *
 * RDS IAM auth tokens are valid for 15 minutes, therefore the pool's max lifetime
 * (`DatabaseProps.maxLifetimeMs`) must be configured below the token lifetime — e.g. 14 minutes —
 * so no connection outlives its credentials.
 */
class RdsHikariCredentialsProvider(
    private val hostname: String,
    private val port: Int,
    private val username: String,
    private val region: Region,
    private val credentialsProvider: AwsCredentialsProvider,
) : HikariCredentialsProvider {

    companion object {
        fun extractHostAndPort(jdbcUrl: String): Pair<String, Int> {
            val uri = URI(jdbcUrl.removePrefix("jdbc:"))
            val host = uri.host
            val port = uri.port
            return host to port
        }
    }

    private val rdsUtilities = RdsUtilities.builder()
        .region(region)
        .credentialsProvider(credentialsProvider)
        .build()

    override fun getCredentials(): Credentials {
        val token = rdsUtilities.generateAuthenticationToken(
            GenerateAuthenticationTokenRequest.builder()
                .hostname(hostname)
                .port(port)
                .username(username)
                .build()
        )
        return Credentials(username, token)
    }
}

