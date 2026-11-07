package ovh.gabrielhuav.flasklogin.data.model

import com.google.gson.annotations.SerializedName

data class AuthRequest(
    val username: String,
    val password: String
)

data class AuthResponse(
    val status: String,
    val message: String? = null,
    val token: String? = null,
    @SerializedName("user_id") val userId: Int? = null,
    val username: String? = null,
    val role: String? = null
)

data class UserDto(
    val id: Int,
    val username: String,
    val role: String,
    @SerializedName("created_at") val createdAt: String? = null
)

data class SingleUserResponse(
    val status: String,
    val message: String? = null,
    val token: String? = null,
    val user: UserDto? = null
)

data class UsersListResponse(
    val status: String,
    val users: List<UserDto> = emptyList()
)

data class UpdateProfileRequest(
    val username: String? = null,
    val password: String? = null
)

data class AdminCreateUserRequest(
    val username: String,
    val password: String,
    val role: String = "user"
)

data class AdminUpdateUserRequest(
    val username: String? = null,
    val password: String? = null,
    val role: String? = null
)

data class TaskItem(
    val id: Int,
    val title: String,
    val description: String = "",
    val status: String = "pending",
    @SerializedName("created_at") val createdAt: String? = null,
    @SerializedName("user_id") val userId: Int = 0
)

data class TaskRequest(
    val title: String,
    val description: String? = null,
    val status: String? = null
)

data class TasksListResponse(
    val status: String,
    val tasks: List<TaskItem> = emptyList()
)

data class SingleTaskResponse(
    val status: String,
    val message: String? = null,
    val task: TaskItem? = null
)
