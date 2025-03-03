package com.stratocloud.cart;

import com.stratocloud.audit.AuditLogContext;
import com.stratocloud.audit.AuditObject;
import com.stratocloud.cart.cmd.CreateCartItemCmd;
import com.stratocloud.cart.cmd.DeleteCartItemsCmd;
import com.stratocloud.cart.cmd.SubmitCartItemsCmd;
import com.stratocloud.cart.cmd.UpdateCartItemCmd;
import com.stratocloud.cart.query.DescribeAllCartItemsResponse;
import com.stratocloud.cart.query.DescribeCartItemsRequest;
import com.stratocloud.cart.query.NestedCartItemResponse;
import com.stratocloud.cart.response.CreateCartItemResponse;
import com.stratocloud.cart.response.DeleteCartItemsResponse;
import com.stratocloud.cart.response.SubmitCartItemsResponse;
import com.stratocloud.cart.response.UpdateCartItemResponse;
import com.stratocloud.request.BatchJobParameters;
import com.stratocloud.exceptions.EntityNotFoundException;
import com.stratocloud.exceptions.StratoException;
import com.stratocloud.job.AsyncJobService;
import com.stratocloud.job.JobHandler;
import com.stratocloud.job.JobHandlerRegistry;
import com.stratocloud.job.cmd.RunAsyncJobCmd;
import com.stratocloud.jpa.entities.EntityUtil;
import com.stratocloud.repository.CartItemRepository;
import com.stratocloud.utils.JSON;
import com.stratocloud.utils.Utils;
import com.stratocloud.validate.ValidateRequest;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class CartServiceImpl implements CartService {

    private final CartItemRepository cartItemRepository;

    private final AsyncJobService asyncJobService;

    public CartServiceImpl(CartItemRepository cartItemRepository, AsyncJobService asyncJobService) {
        this.cartItemRepository = cartItemRepository;
        this.asyncJobService = asyncJobService;
    }

    @Override
    @ValidateRequest
    @Transactional(readOnly = true)
    public Page<NestedCartItemResponse> describeCartItems(DescribeCartItemsRequest request) {
        Page<CartItem> page = cartItemRepository.page(request.getSearch(), request.getPageable());
        return page.map(this::toNestedCartItemResponse);
    }

    private NestedCartItemResponse toNestedCartItemResponse(CartItem cartItem) {
        NestedCartItemResponse r = new NestedCartItemResponse();
        EntityUtil.copyBasicFields(cartItem, r);
        r.setJobType(cartItem.getJobType());
        r.setJobTypeName(cartItem.getJobTypeName());
        r.setJobParameters(cartItem.getJobParameters());
        r.setSummary(cartItem.getSummary());
        return r;
    }

    @Override
    @ValidateRequest
    @Transactional
    public CreateCartItemResponse createCartItem(CreateCartItemCmd cmd) {
        CartItem cartItem = new CartItem(cmd.getJobType(), cmd.getJobParameters());

        cartItem = cartItemRepository.save(cartItem);

        addAuditObject(cartItem);

        return new CreateCartItemResponse(cartItem.getId());
    }

    private static void addAuditObject(CartItem cartItem) {
        AuditLogContext.current().addAuditObject(
                new AuditObject(cartItem.getId().toString(), cartItem.getJobTypeName())
        );
    }

    @Override
    @ValidateRequest
    @Transactional
    public UpdateCartItemResponse updateCartItem(UpdateCartItemCmd cmd) {
        CartItem cartItem = cartItemRepository.findById(cmd.getCartItemId()).orElseThrow(
                () -> new EntityNotFoundException("Cart item not found.")
        );

        addAuditObject(cartItem);

        cartItemRepository.save(cartItem);
        return new UpdateCartItemResponse();
    }

    @Override
    @ValidateRequest
    @Transactional
    public DeleteCartItemsResponse deleteCartItems(DeleteCartItemsCmd cmd) {
        cmd.getCartItemIds().forEach(this::deleteCartItem);
        return new DeleteCartItemsResponse();
    }

    private void deleteCartItem(Long cartItemId) {
        CartItem cartItem = cartItemRepository.findById(cartItemId).orElseThrow(
                () -> new EntityNotFoundException("Cart item not found.")
        );

        addAuditObject(cartItem);

        cartItemRepository.delete(cartItem);
    }


    @Override
    @ValidateRequest
    @Transactional
    public SubmitCartItemsResponse submitCartItems(SubmitCartItemsCmd cmd) {
        List<CartItem> cartItems = cartItemRepository.findAllById(cmd.getCartItemIds());

        cartItems.forEach(CartServiceImpl::addAuditObject);

        if(Utils.isEmpty(cartItems))
            return new SubmitCartItemsResponse();

        Map<String, List<CartItem>> cartItemsGroupedByJobType = cartItems.stream().collect(
                Collectors.groupingBy(CartItem::getJobType)
        );

        List<RunAsyncJobCmd> runAsyncJobCmdList = new ArrayList<>();

        for (var entry : cartItemsGroupedByJobType.entrySet()) {
            JobHandler<?> jobHandler = JobHandlerRegistry.getJobHandler(entry.getKey());
            Class<?> jobParameterClass = jobHandler.getParameterClass();

            if(Utils.isEmpty(entry.getValue()))
                continue;

            if(BatchJobParameters.class.isAssignableFrom(jobParameterClass)){
                BatchJobParameters batchJobParameters = null;

                for (CartItem cartItem : entry.getValue()) {
                    BatchJobParameters jobParameters = (BatchJobParameters) jobHandler.toTypedJobParameters(
                            cartItem.getJobParameters()
                    );

                    if(batchJobParameters == null)
                        batchJobParameters = jobParameters;
                    else
                        batchJobParameters.merge(jobParameters);
                }

                RunAsyncJobCmd runAsyncJobCmd = new RunAsyncJobCmd();
                runAsyncJobCmd.setJobType(entry.getKey());
                runAsyncJobCmd.setJobParameters(JSON.toMap(batchJobParameters));
                runAsyncJobCmdList.add(runAsyncJobCmd);
            } else {
                for (CartItem cartItem : entry.getValue()) {
                    RunAsyncJobCmd runAsyncJobCmd = new RunAsyncJobCmd();
                    runAsyncJobCmd.setJobType(entry.getKey());
                    runAsyncJobCmd.setJobParameters(cartItem.getJobParameters());
                    runAsyncJobCmdList.add(runAsyncJobCmd);
                }
            }
        }

        if(Utils.isEmpty(runAsyncJobCmdList))
            throw new StratoException("No command added");

        for (RunAsyncJobCmd runAsyncJobCmd : runAsyncJobCmdList) {
            asyncJobService.runAsyncJob(runAsyncJobCmd);
        }

        cartItemRepository.deleteAll(cartItems);

        return new SubmitCartItemsResponse();
    }


    @Override
    @ValidateRequest
    @Transactional(readOnly = true)
    public DescribeAllCartItemsResponse describeAllCartItems(DescribeCartItemsRequest request) {
        List<CartItem> cartItems = cartItemRepository.findByFilters(request.getSearch());
        return new DescribeAllCartItemsResponse(
                cartItems.stream().map(this::toNestedCartItemResponse).toList()
        );
    }
}
