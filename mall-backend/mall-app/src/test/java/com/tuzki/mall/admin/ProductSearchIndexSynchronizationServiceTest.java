package com.tuzki.mall.admin;

import com.tuzki.mall.admin.product.search.ProductSearchIndexService;
import com.tuzki.mall.admin.product.search.ProductSearchIndexSynchronizationService;
import org.junit.jupiter.api.Test;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

class ProductSearchIndexSynchronizationServiceTest {

    @Test
    void syncProductAfterCommitRunsImmediatelyWhenNoTransactionSynchronization() {
        ProductSearchIndexService productSearchIndexService = mock(ProductSearchIndexService.class);
        ProductSearchIndexSynchronizationService service = new ProductSearchIndexSynchronizationService(productSearchIndexService);

        service.syncProductAfterCommit(100L);

        verify(productSearchIndexService).syncProduct(100L);
    }

    @Test
    void deleteProductAfterCommitRunsImmediatelyWhenNoTransactionSynchronization() {
        ProductSearchIndexService productSearchIndexService = mock(ProductSearchIndexService.class);
        ProductSearchIndexSynchronizationService service = new ProductSearchIndexSynchronizationService(productSearchIndexService);

        service.deleteProductAfterCommit(100L);

        verify(productSearchIndexService).deleteProduct(100L);
    }

    @Test
    void nullProductIdDoesNothing() {
        ProductSearchIndexService productSearchIndexService = mock(ProductSearchIndexService.class);
        ProductSearchIndexSynchronizationService service = new ProductSearchIndexSynchronizationService(productSearchIndexService);

        service.syncProductAfterCommit(null);
        service.deleteProductAfterCommit(null);

        verifyNoInteractions(productSearchIndexService);
    }
}
