package com.thehotelmedia.android.customClasses

import android.content.Context
import android.content.SharedPreferences

object ViewedStoriesManager {
    private const val PREFS_NAME = "viewed_stories_prefs"
    private const val KEY_VIEWED_STORIES = "viewed_story_ids"

    /**
     * For multi-account support we scope viewed stories per logged-in user.
     * Each account gets its own key in the same SharedPreferences file.
     */
    private fun getUserKey(userId: String): String {
        return if (userId.isNotEmpty()) {
            "${KEY_VIEWED_STORIES}_$userId"
        } else {
            // Fallback to legacy key if we don't have a user id for some reason
            KEY_VIEWED_STORIES
        }
    }
    
    /**
     * Mark a story as viewed by the current user
     */
    fun markStoryAsViewed(context: Context, userId: String, storyId: String) {
        if (storyId.isEmpty()) return
        
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val key = getUserKey(userId)
        val viewedStories = prefs.getStringSet(key, mutableSetOf())?.toMutableSet() ?: mutableSetOf()
        viewedStories.add(storyId)
        prefs.edit().putStringSet(key, viewedStories).apply()
        
        android.util.Log.d(
            "ViewedStoriesManager",
            "Marked story $storyId as viewed for user '$userId'. Total viewed for this user: ${viewedStories.size}"
        )
    }
    
    /**
     * Check if a story has been viewed by the current user
     */
    fun isStoryViewed(context: Context, userId: String, storyId: String): Boolean {
        if (storyId.isEmpty()) return false
        
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val key = getUserKey(userId)
        val viewedStories = prefs.getStringSet(key, mutableSetOf()) ?: mutableSetOf()
        return viewedStories.contains(storyId)
    }
    
    /**
     * Mark all stories for a user as viewed (when all stories have been viewed)
     */
    fun markAllStoriesAsViewed(context: Context, userId: String, storyIds: List<String>) {
        if (storyIds.isEmpty()) return
        
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val key = getUserKey(userId)
        val viewedStories = prefs.getStringSet(key, mutableSetOf())?.toMutableSet() ?: mutableSetOf()
        viewedStories.addAll(storyIds.filter { it.isNotEmpty() })
        prefs.edit().putStringSet(key, viewedStories).apply()
        
        android.util.Log.d(
            "ViewedStoriesManager",
            "Marked ${storyIds.size} stories as viewed for user '$userId'. Total viewed for this user: ${viewedStories.size}"
        )
    }
    
    /**
     * Check if all stories in a list have been viewed
     */
    fun areAllStoriesViewed(context: Context, userId: String, storyIds: List<String>): Boolean {
        if (storyIds.isEmpty()) return false
        
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val key = getUserKey(userId)
        val viewedStories = prefs.getStringSet(key, mutableSetOf()) ?: mutableSetOf()
        
        return storyIds.all { storyId ->
            storyId.isEmpty() || viewedStories.contains(storyId)
        }
    }
    
    /**
     * Clear viewed stories (useful for logout or testing)
     */
    fun clearViewedStories(context: Context, userId: String) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val key = getUserKey(userId)
        prefs.edit().remove(key).apply()
        android.util.Log.d("ViewedStoriesManager", "Cleared all viewed stories for user '$userId'")
    }
}

