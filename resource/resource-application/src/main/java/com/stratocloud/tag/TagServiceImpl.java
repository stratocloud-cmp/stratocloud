package com.stratocloud.tag;
import com.stratocloud.audit.AuditLogContext;
import com.stratocloud.audit.AuditObject;
import com.stratocloud.jpa.entities.EntityUtil;
import com.stratocloud.repository.TagValueRepository;
import com.stratocloud.tag.query.DescribeTagValuesRequest;
import com.stratocloud.tag.query.NestedTagValueResponse;
import com.stratocloud.validate.ValidateRequest;
import org.springframework.data.domain.Pageable;

import com.stratocloud.repository.TagEntryRepository;
import com.stratocloud.tag.cmd.*;
import com.stratocloud.tag.query.DescribeTagEntriesRequest;
import com.stratocloud.tag.query.NestedTagEntryResponse;
import com.stratocloud.tag.response.*;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class TagServiceImpl implements TagService{

    private final TagEntryRepository repository;

    private final TagValueRepository tagValueRepository;

    public TagServiceImpl(TagEntryRepository repository, TagValueRepository tagValueRepository) {
        this.repository = repository;
        this.tagValueRepository = tagValueRepository;
    }

    @Override
    @Transactional
    @ValidateRequest
    public CreateTagEntryResponse createTagEntry(CreateTagEntryCmd cmd) {
        String resourceCategory = cmd.getResourceCategory();
        String tagKey = cmd.getTagKey();
        String tagKeyName = cmd.getTagKeyName();
        String description = cmd.getDescription();
        Boolean requiredWhenCreating = cmd.getRequiredWhenCreating();
        Boolean requiredWhenFiltering = cmd.getRequiredWhenFiltering();
        Boolean userGroupTaggable = cmd.getUserGroupTaggable();


        ResourceTagEntry tagEntry = ResourceTagEntry.builder()
                .resourceCategory(resourceCategory)
                .tagKey(tagKey).tagKeyName(tagKeyName)
                .description(description)
                .requiredWhenCreating(requiredWhenCreating)
                .requiredWhenFiltering(requiredWhenFiltering)
                .userGroupTaggable(userGroupTaggable)
                .build();

        tagEntry = repository.save(tagEntry);

        addAuditObject(tagEntry);

        return new CreateTagEntryResponse(tagEntry.getId());
    }

    @Override
    @Transactional
    @ValidateRequest
    public UpdateTagEntryResponse updateTagEntry(UpdateTagEntryCmd cmd) {
        Long tagEntryId = cmd.getTagEntryId();
        String resourceCategory = cmd.getResourceCategory();
        String tagKeyName = cmd.getTagKeyName();
        String description = cmd.getDescription();
        Boolean requiredWhenCreating = cmd.getRequiredWhenCreating();
        Boolean requiredWhenFiltering = cmd.getRequiredWhenFiltering();
        Boolean userGroupTaggable = cmd.getUserGroupTaggable();

        ResourceTagEntry tagEntry = repository.findTagEntry(tagEntryId);

        addAuditObject(tagEntry);

        tagEntry.update(resourceCategory, tagKeyName, description, requiredWhenCreating,
                requiredWhenFiltering, userGroupTaggable);

        repository.save(tagEntry);

        return new UpdateTagEntryResponse();
    }

    @Override
    @Transactional
    @ValidateRequest
    public DeleteTagEntriesResponse deleteTagEntries(DeleteTagEntriesCmd cmd) {
        List<Long> tagEntryIds = cmd.getTagEntryIds();

        tagEntryIds.forEach(this::deleteTagEntry);

        return new DeleteTagEntriesResponse();
    }

    private void deleteTagEntry(Long tagEntryId) {
        ResourceTagEntry tagEntry = repository.findTagEntry(tagEntryId);

        addAuditObject(tagEntry);

        repository.delete(tagEntry);
    }

    @Override
    @Transactional
    @ValidateRequest
    public AddTagValueResponse addTagValue(AddTagValueCmd cmd) {
        Long tagEntryId = cmd.getTagEntryId();
        String tagValue = cmd.getTagValue();
        String tagValueName = cmd.getTagValueName();
        int index = cmd.getIndex();
        String description = cmd.getDescription();

        ResourceTagEntry tagEntry = repository.findTagEntry(tagEntryId);

        addAuditObject(tagEntry);


        tagEntry.addValue(tagValue, tagValueName, index, description);

        return new AddTagValueResponse();
    }

    @Override
    @Transactional
    @ValidateRequest
    public RemoveTagValuesResponse removeTagValues(RemoveTagValuesCmd cmd) {
        Long tagEntryId = cmd.getTagEntryId();
        List<Long> tagValueIds = cmd.getTagValueIds();

        ResourceTagEntry tagEntry = repository.findTagEntry(tagEntryId);

        addAuditObject(tagEntry);

        tagValueIds.forEach(tagEntry::removeValueById);

        repository.save(tagEntry);

        return new RemoveTagValuesResponse();
    }

    private static void addAuditObject(ResourceTagEntry tagEntry) {
        AuditLogContext.current().addAuditObject(
                new AuditObject(tagEntry.getTagKey(), tagEntry.getTagKeyName())
        );
    }


    @Override
    @Transactional(readOnly = true)
    @ValidateRequest
    public Page<NestedTagEntryResponse> describeTagEntries(DescribeTagEntriesRequest request) {
        List<Long> entryIds = request.getEntryIds();
        List<String> entryKeys = request.getEntryKeys();

        List<String> resourceCategories = request.getResourceCategories();
        String search = request.getSearch();
        Boolean requiredWhenCreating = request.getRequiredWhenCreating();
        Boolean requiredWhenFiltering = request.getRequiredWhenFiltering();
        Boolean userGroupTaggable = request.getUserGroupTaggable();
        Pageable pageable = request.getPageable();

        TagEntryFilters filters = new TagEntryFilters(entryIds, entryKeys, resourceCategories, search,
                requiredWhenCreating, requiredWhenFiltering, userGroupTaggable);

        Page<ResourceTagEntry> page = repository.page(filters, request.getDisabled(), pageable);

        return page.map(this::toNestedTagEntryResponse);
    }

    @Override
    @Transactional(readOnly = true)
    @ValidateRequest
    public Page<NestedTagValueResponse> describeTagValues(DescribeTagValuesRequest request) {
        String tagEntryKey = request.getTagEntryKey();
        String search = request.getSearch();
        List<String> tagValues = request.getTagValues();
        Pageable pageable = request.getPageable();

        Page<ResourceTagValue> page = tagValueRepository.page(
                tagEntryKey, search, tagValues, pageable
        );

        return page.map(this::toNestedTagValueResponse);
    }

    @Override
    @Transactional
    @ValidateRequest
    public DisableTagEntriesResponse disableTagEntries(DisableTagEntriesCmd cmd) {
        List<ResourceTagEntry> tagEntries = repository.findAllById(cmd.getTagEntryIds());
        tagEntries.forEach(TagServiceImpl::addAuditObject);

        tagEntries.forEach(ResourceTagEntry::disable);
        repository.saveAll(tagEntries);

        return new DisableTagEntriesResponse();
    }

    @Override
    @Transactional
    @ValidateRequest
    public EnableTagEntriesResponse enableTagEntries(EnableTagEntriesCmd cmd) {
        List<ResourceTagEntry> tagEntries = repository.findAllById(cmd.getTagEntryIds());
        tagEntries.forEach(TagServiceImpl::addAuditObject);

        tagEntries.forEach(ResourceTagEntry::enable);
        repository.saveAll(tagEntries);

        return new EnableTagEntriesResponse();
    }

    private NestedTagValueResponse toNestedTagValueResponse(ResourceTagValue resourceTagValue) {
        NestedTagValueResponse nestedTagValueResponse = new NestedTagValueResponse();

        EntityUtil.copyBasicFields(resourceTagValue, nestedTagValueResponse);

        nestedTagValueResponse.setTagEntryId(resourceTagValue.getEntry().getId());
        nestedTagValueResponse.setTagKey(resourceTagValue.getEntry().getTagKey());
        nestedTagValueResponse.setTagKeyName(resourceTagValue.getEntry().getTagKeyName());

        nestedTagValueResponse.setTagValue(resourceTagValue.getTagValue());
        nestedTagValueResponse.setTagValueName(resourceTagValue.getTagValueName());
        nestedTagValueResponse.setDescription(resourceTagValue.getDescription());


        return nestedTagValueResponse;
    }

    private NestedTagEntryResponse toNestedTagEntryResponse(ResourceTagEntry resourceTagEntry) {
        NestedTagEntryResponse response = new NestedTagEntryResponse();

        EntityUtil.copyBasicFields(resourceTagEntry, response);

        response.setResourceCategory(resourceTagEntry.getResourceCategory());

        response.setTagKey(resourceTagEntry.getTagKey());
        response.setTagKeyName(resourceTagEntry.getTagKeyName());

        response.setDescription(resourceTagEntry.getDescription());

        response.setRequiredWhenCreating(resourceTagEntry.getRequiredWhenCreating());
        response.setRequiredWhenFiltering(resourceTagEntry.getRequiredWhenFiltering());

        response.setUserGroupTaggable(resourceTagEntry.getUserGroupTaggable());

        return response;
    }
}
