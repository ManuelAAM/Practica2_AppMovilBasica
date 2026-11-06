package ovh.gabrielhuav.flasklogin.shared.network

import io.ktor.client.HttpClient

expect fun createPlatformHttpClient(): HttpClient
