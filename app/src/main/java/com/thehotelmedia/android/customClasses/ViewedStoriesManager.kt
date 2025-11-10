package com.thehotelmedia.android.customClasses

import android.content.Context
import android.content.SharedPreferences

/**
 * Manages tracking of viewed stories for immediate UI updates.
 * This ensures the blue ring disappears immediately after viewing stories,
 * even before backend data refreshes.
 */
object ViewedStoriesManager {
    private const val PREFS_NAME = "viewed_stories_prefs"
    private const val KEY_VIEWED_STORIES = "viewed_story_ids"
    
    /**
     * Mark a story as viewed by the current user
     */
    fun markStoryAsViewed(context: Context, storyId: String) {
        if (storyId.isEmpty()) return
        
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val viewedStories = prefs.getStringSet(KEY_VIEWED_STORIES, mutableSetOf())?.toMutableSet() ?: mutableSetOf()
        viewedStories.add(storyId)
        prefs.edit().putStringSet(KEY_VIEWED_STORIES, viewedStories).apply()
        
        android.util.Log.d("ViewedStoriesManager", "Marked story $storyId as viewed. Total viewed: ${viewedStories.size}")
    }
    
    /**
     * Check if a story has been viewed by the current user
     */
    fun isStoryViewed(context: Context, storyId: String): Boolean {
        if (storyId.isEmpty()) return false
        
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val viewedStories = prefs.getStringSet(KEY_VIEWED_STORIES, mutableSetOf()) ?: mutableSetOf()
        return viewedStories.contains(storyId)
    }
    
    /**
     * Mark all stories for a user as viewed (when all stories have been viewed)
     */
    fun markAllStoriesAsViewed(context: Context, storyIds: List<String>) {
        if (storyIds.isEmpty()) return
        
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val viewedStories = prefs.getStringSet(KEY_VIEWED_STORIES, mutableSetOf())?.toMutableSet() ?: mutableSetOf()
        viewedStories.addAll(storyIds.filter { it.isNotEmpty() })
        prefs.edit().putStringSet(KEY_VIEWED_STORIES, viewedStories).apply()
        
        android.util.Log.d("ViewedStoriesManager", "Marked ${storyIds.size} stories as viewed. Total viewed: ${viewedStories.size}")
    }
    
    /**
     * Check if all stories in a list have been viewed
     */
    fun areAllStoriesViewed(context: Context, storyIds: List<String>): Boolean {
        if (storyIds.isEmpty()) return false
        
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val viewedStories = prefs.getStringSet(KEY_VIEWED_STORIES, mutableSetOf()) ?: mutableSetOf()
        
        return storyIds.all { storyId ->
            storyId.isEmpty() || viewedStories.contains(storyId)
        }
    }
    
    /**
     * Clear viewed stories (useful for logout or testing)
     */
    fun clearViewedStories(context: Context) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().remove(KEY_VIEWED_STORIES).apply()
        android.util.Log.d("ViewedStoriesManager", "Cleared all viewed stories")
    }
}

