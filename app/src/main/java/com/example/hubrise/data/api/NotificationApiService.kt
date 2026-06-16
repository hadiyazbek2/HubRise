package com.example.hubrise.data.api

import com.example.hubrise.data.model.NotificationItem
import com.example.hubrise.data.model.UnreadCountResponse
import retrofit2.Response
import retrofit2.http.GET

interface NotificationApiService {

    @GET("api/notifications/")
    suspend fun getNotifications(): Response<List<NotificationItem>>

    @GET("api/notifications/unread-count/")
    suspend fun getUnreadCount(): Response<UnreadCountResponse>
}
