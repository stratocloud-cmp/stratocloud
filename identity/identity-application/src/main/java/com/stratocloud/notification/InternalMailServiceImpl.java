package com.stratocloud.notification;

import com.stratocloud.exceptions.EntityNotFoundException;
import com.stratocloud.jpa.entities.EntityUtil;
import com.stratocloud.notification.cmd.MarkInternalMailsReadCmd;
import com.stratocloud.notification.internal.InternalMail;
import com.stratocloud.notification.query.DescribeInternalMailRequest;
import com.stratocloud.notification.query.NestedInternalMail;
import com.stratocloud.notification.response.MarkInternalMailsReadResponse;
import com.stratocloud.repository.InternalMailRepository;
import com.stratocloud.utils.Utils;
import com.stratocloud.validate.ValidateRequest;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class InternalMailServiceImpl implements InternalMailService{

    private final InternalMailRepository repository;

    public InternalMailServiceImpl(InternalMailRepository repository) {
        this.repository = repository;
    }

    @Override
    @Transactional(readOnly = true)
    @ValidateRequest
    public Page<NestedInternalMail> describeInternalMails(DescribeInternalMailRequest request) {
        Page<InternalMail> page = repository.page(
                request.getSearch(),
                request.getPageable()
        );
        return page.map(this::toNestedInternalMail);
    }

    private NestedInternalMail toNestedInternalMail(InternalMail internalMail) {
        NestedInternalMail nestedInternalMail = new NestedInternalMail();

        EntityUtil.copyBasicFields(internalMail, nestedInternalMail);

        nestedInternalMail.setEventId(internalMail.getEventId());
        nestedInternalMail.setReceiverUserId(internalMail.getReceiverUserId());
        nestedInternalMail.setMessage(internalMail.getMessage());
        nestedInternalMail.setRead(internalMail.isRead());

        return nestedInternalMail;
    }

    @Override
    @Transactional
    @ValidateRequest
    public MarkInternalMailsReadResponse markInternalMailsRead(MarkInternalMailsReadCmd cmd) {
        if(Utils.isNotEmpty(cmd.getInternalMailIds()))
            cmd.getInternalMailIds().forEach(this::markInternalMailRead);

        return new MarkInternalMailsReadResponse();
    }

    private void markInternalMailRead(Long internalMailId) {
        InternalMail internalMail = repository.findById(internalMailId).orElseThrow(
                () -> new EntityNotFoundException("Internal mail not found")
        );

        internalMail.markRead();

        repository.save(internalMail);
    }
}
