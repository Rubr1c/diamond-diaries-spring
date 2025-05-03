package dev.rubric.journalspring.service;

import dev.rubric.journalspring.models.Tag;
import dev.rubric.journalspring.repository.TagRepository;
import jakarta.annotation.PostConstruct;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class TagInitializerService {

    private final TagRepository tagRepository;

    public TagInitializerService(TagRepository tagRepository) {
        this.tagRepository = tagRepository;
    }

    @PostConstruct
    public void initTags() {
        List<String> predefinedTags = List.of(
                // Daily Life
                "Morning Thoughts", "Daily Reflection", "Achievements", "Challenges", "Gratitude", "To-Do List",

                // Emotions & Feelings
                "Happiness", "Sadness", "Frustration", "Excitement", "Anxiety", "Motivation", "Love", "Peaceful Moments",

                // Health & Well-being
                "Exercise", "Meditation", "Self-care", "Healthy Eating", "Sleep Tracking", "Mental Health", "Personal Growth",

                // Social & Relationships
                "Family", "Friendship", "Love Life", "Social Events", "Heartbreak", "Loneliness", "Acts of Kindness", "Conflicts",

                // Work & Productivity
                "Career Progress", "Work Challenges", "Productivity", "Creative Ideas", "New Skills", "Learning", "Side Projects",

                // Spiritual & Reflection
                "Faith", "Prayer", "Quran Reflection", "Personal Duas", "Self-Improvement", "Good Deeds",

                // Hobbies & Interests
                "Books", "Music", "Movies", "Photography", "Gaming", "Coding", "Art & Drawing", "Cooking", "Gardening",

                // Travel & Adventure
                "Places Visited", "Bucket List", "Nature Walks", "Cultural Experiences", "Memorable Trips",

                // Random & Miscellaneous
                "Dreams", "Funny Moments", "Random Thoughts", "Deep Conversations", "Overheard Conversations",

                // Financial & Planning
                "Budgeting", "Savings Goals", "Investment", "Minimalism", "Financial Reflections"
        );

        for (String tagName : predefinedTags) {
            if (tagRepository.findByName(tagName).isEmpty()) {
                tagRepository.save(new Tag(tagName));
            }
        }
    }
}
