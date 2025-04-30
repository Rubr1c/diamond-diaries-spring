package dev.rubric.journalspring.service;

import dev.rubric.journalspring.models.Tag;
import dev.rubric.journalspring.repository.TagRepository;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

@Service
public class TagService {

    private final TagRepository tagRepository;

    public TagService(TagRepository tagRepository) {
        this.tagRepository = tagRepository;
    }

    public List<String> getAllTagNames() {
        return StreamSupport.stream(
                tagRepository.findAll().spliterator(), false)
                .map(Tag::getName)
                .collect(Collectors.toList());
    }
}
