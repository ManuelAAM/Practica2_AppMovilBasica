package ovh.gabrielhuav.flasklogin.data.api

import ovh.gabrielhuav.flasklogin.data.model.AdminCreateUserRequest
import ovh.gabrielhuav.flasklogin.data.model.AdminUpdateUserRequest
import ovh.gabrielhuav.flasklogin.data.model.AuthRequest
import ovh.gabrielhuav.flasklogin.data.model.AuthResponse
import ovh.gabrielhuav.flasklogin.data.model.SingleTaskResponse
import ovh.gabrielhuav.flasklogin.data.model.SingleUserResponse
import ovh.gabrielhuav.flasklogin.data.model.TaskRequest
import ovh.gabrielhuav.flasklogin.data.model.TasksListResponse
import ovh.gabrielhuav.flasklogin.data.model.UpdateProfileRequest
import ovh.gabrielhuav.flasklogin.data.model.UsersListResponse
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.PUT
import retrofit2.http.Path

interface ApiService {

    @POST("register")
    suspend fun register(@Body request: AuthRequest): Response<AuthResponse>

    @POST("login")
    suspend fun login(@Body request: AuthRequest): Response<AuthResponse>

    @GET("profile")
    suspend fun getProfile(): Response<SingleUserResponse>

    @PUT("profile")
    suspend fun updateProfile(@Body request: UpdateProfileRequest): Response<SingleUserResponse>

    @GET("users")
    suspend fun getAllUsers(): Response<UsersListResponse>

    @GET("users")
    suspend fun getUsers(): Response<UsersListResponse>

    @GET("users/{id}")
    suspend fun getUserById(@Path("id") id: Int): Response<SingleUserResponse>

    @POST("users")
    suspend fun adminCreateUser(@Body request: AdminCreateUserRequest): Response<SingleUserResponse>

    @POST("users")
    suspend fun createUser(@Body request: AdminCreateUserRequest): Response<SingleUserResponse>

    @PUT("users/{id}")
    suspend fun adminUpdateUser(@Path("id") id: Int, @Body request: AdminUpdateUserRequest): Response<SingleUserResponse>

    @PUT("users/{id}")
    suspend fun updateUser(@Path("id") id: Int, @Body request: AdminUpdateUserRequest): Response<SingleUserResponse>

    @DELETE("users/{id}")
    suspend fun adminDeleteUser(@Path("id") id: Int): Response<Map<String, String>>

    @DELETE("users/{id}")
    suspend fun deleteUser(@Path("id") id: Int): Response<Map<String, String>>

    @GET("tasks")
    suspend fun getTasks(): Response<TasksListResponse>

    @GET("tasks/{id}")
    suspend fun getTaskById(@Path("id") id: Int): Response<SingleTaskResponse>

    @POST("tasks")
    suspend fun createTask(@Body request: TaskRequest): Response<SingleTaskResponse>

    @PUT("tasks/{id}")
    suspend fun updateTask(@Path("id") id: Int, @Body request: TaskRequest): Response<SingleTaskResponse>

    @DELETE("tasks/{id}")
    suspend fun deleteTask(@Path("id") id: Int): Response<Map<String, String>>
}
