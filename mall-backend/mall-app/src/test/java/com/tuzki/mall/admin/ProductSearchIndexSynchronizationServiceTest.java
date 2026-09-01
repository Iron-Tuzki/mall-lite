package com.tuzki.mall.admin;

import com.tuzki.mall.admin.product.search.ProductSearchIndexChangedEvent;
import com.tuzki.mall.admin.product.search.ProductSearchIndexChangedEventType;
import com.tuzki.mall.admin.product.search.ProductSearchIndexEventSender;
import com.tuzki.mall.admin.product.search.ProductSearchIndexSynchronizationService;
import org.junit.jupiter.api.Test;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

class ProductSearchIndexSynchronizationServiceTest {

    @Test
    void syncProductAfterCommitSendsSyncEventImmediatelyWhenNoTransactionSynchronization() {
        ProductSearchIndexEventSender eventSender = mock(ProductSearchIndexEventSender.class);
        ProductSearchIndexSynchronizationService service = new ProductSearchIndexSynchronizationService(eventSender);

        service.syncProductAfterCommit(100L);

        verify(eventSender).send(productEvent(100L, ProductSearchIndexChangedEventType.SYNC));
    }

    @Test
    void deleteProductAfterCommitSendsDeleteEventImmediatelyWhenNoTransactionSynchronization() {
        ProductSearchIndexEventSender eventSender = mock(ProductSearchIndexEventSender.class);
        ProductSearchIndexSynchronizationService service = new ProductSearchIndexSynchronizationService(eventSender);

        service.deleteProductAfterCommit(100L);

        verify(eventSender).send(productEvent(100L, ProductSearchIndexChangedEventType.DELETE));
    }

    @Test
    void syncProductEventIsWrittenImmediatelyWhenTransactionSynchronizationIsActive() {
        ProductSearchIndexEventSender eventSender = mock(ProductSearchIndexEventSender.class);
        ProductSearchIndexSynchronizationService service = new ProductSearchIndexSynchronizationService(eventSender);
        TransactionSynchronizationManager.initSynchronization();
        try {
            service.syncProductAfterCommit(100L);

            verify(eventSender).send(productEvent(100L, ProductSearchIndexChangedEventType.SYNC));
        } finally {
            TransactionSynchronizationManager.clearSynchronization();
        }
    }

    @Test
    void nullProductIdDoesNothing() {
        ProductSearchIndexEventSender eventSender = mock(ProductSearchIndexEventSender.class);
        ProductSearchIndexSynchronizationService service = new ProductSearchIndexSynchronizationService(eventSender);

        service.syncProductAfterCommit(null);
        service.deleteProductAfterCommit(null);

        verifyNoInteractions(eventSender);
    }

    private ProductSearchIndexChangedEvent productEvent(Long productId, ProductSearchIndexChangedEventType eventType) {
        return argThat(event -> productId.equals(event.productId()) && eventType == event.eventType());
    }
}
