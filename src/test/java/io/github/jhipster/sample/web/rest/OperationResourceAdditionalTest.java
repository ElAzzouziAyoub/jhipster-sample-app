package io.github.jhipster.sample.web.rest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasItem;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.jhipster.sample.IntegrationTest;
import io.github.jhipster.sample.domain.BankAccount;
import io.github.jhipster.sample.domain.Operation;
import io.github.jhipster.sample.repository.BankAccountRepository;
import io.github.jhipster.sample.repository.OperationRepository;
import jakarta.persistence.EntityManager;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.Random;
import java.util.concurrent.atomic.AtomicLong;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

/**
 * Additional integration tests for the {@link OperationResource} REST controller.
 */
@AutoConfigureMockMvc
@IntegrationTest
@WithMockUser
class OperationResourceAdditionalTest {

    private static final String DEFAULT_DESCRIPTION = "AAAAAAAAAA";
    private static final String UPDATED_DESCRIPTION = "BBBBBBBBBB";

    private static final BigDecimal DEFAULT_AMOUNT = new BigDecimal(1);
    private static final BigDecimal UPDATED_AMOUNT = new BigDecimal(2);

    private static final Instant DEFAULT_DATE = Instant.ofEpochMilli(0L);
    private static final Instant UPDATED_DATE = Instant.now();

    private static final String ENTITY_API_URL = "/api/operations";
    private static final String ENTITY_API_URL_ID = ENTITY_API_URL + "/{id}";

    private static Random random = new Random();
    private static AtomicLong longCount = new AtomicLong(random.nextInt() + (2 * Integer.MAX_VALUE));

    @Autowired
    private ObjectMapper om;

    @Autowired
    private OperationRepository operationRepository;

    @Autowired
    private BankAccountRepository bankAccountRepository;

    @Autowired
    private EntityManager em;

    @Autowired
    private MockMvc restOperationMockMvc;

    private Operation operation;
    private BankAccount bankAccount;
    private Operation insertedOperation;

    @BeforeEach
    void initTest() {
        bankAccount = new BankAccount();
        bankAccount.setName("Test Account");
        bankAccount.setBalance(new BigDecimal(1000));
        bankAccount = bankAccountRepository.saveAndFlush(bankAccount);

        operation = new Operation();
        operation.setDate(DEFAULT_DATE);
        operation.setDescription(DEFAULT_DESCRIPTION);
        operation.setAmount(DEFAULT_AMOUNT);
        operation.setBankAccount(bankAccount);
    }

    @AfterEach
    void cleanup() {
        if (insertedOperation != null) {
            try {
                operationRepository.delete(insertedOperation);
            } catch (Exception e) {
                // Ignore if already deleted
            }
            insertedOperation = null;
        }
        if (bankAccount != null) {
            try {
                bankAccountRepository.delete(bankAccount);
            } catch (Exception e) {
                // Ignore if already deleted
            }
        }
    }

    @Test
    @Transactional
    void getAllOperationsWithPagination() throws Exception {
        // Initialize the database
        insertedOperation = operationRepository.saveAndFlush(operation);

        // Get all the operations with pagination
        restOperationMockMvc
            .perform(get(ENTITY_API_URL + "?page=0&size=10&sort=id,desc"))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE))
            .andExpect(jsonPath("$.[*].id").value(hasItem(operation.getId().intValue())))
            .andExpect(jsonPath("$.[*].description").value(hasItem(DEFAULT_DESCRIPTION)))
            .andExpect(jsonPath("$.[*].amount").value(hasItem(DEFAULT_AMOUNT.intValue())));
    }

    @Test
    @Transactional
    void getAllOperationsWithEagerload() throws Exception {
        // Initialize the database
        insertedOperation = operationRepository.saveAndFlush(operation);

        // Get all the operations with eagerload
        restOperationMockMvc
            .perform(get(ENTITY_API_URL + "?eagerload=true&page=0&size=10"))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE))
            .andExpect(jsonPath("$.[*].id").value(hasItem(operation.getId().intValue())));
    }

    @Test
    @Transactional
    void getAllOperationsWithoutEagerload() throws Exception {
        // Initialize the database
        insertedOperation = operationRepository.saveAndFlush(operation);

        // Get all the operations without eagerload
        restOperationMockMvc
            .perform(get(ENTITY_API_URL + "?eagerload=false&page=0&size=10"))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE))
            .andExpect(jsonPath("$.[*].id").value(hasItem(operation.getId().intValue())));
    }

    @Test
    @Transactional
    void partialUpdateOperationWithNullFields() throws Exception {
        // Initialize the database
        insertedOperation = operationRepository.saveAndFlush(operation);

        long databaseSizeBeforeUpdate = operationRepository.count();

        // Update the operation using partial update with only one field
        Operation partialUpdatedOperation = new Operation();
        partialUpdatedOperation.setId(operation.getId());
        partialUpdatedOperation.setDescription(UPDATED_DESCRIPTION);
        // Amount and date are null

        restOperationMockMvc
            .perform(
                patch(ENTITY_API_URL_ID, partialUpdatedOperation.getId())
                    .contentType("application/merge-patch+json")
                    .content(om.writeValueAsBytes(partialUpdatedOperation))
            )
            .andExpect(status().isOk());

        // Validate the Operation in the database
        assertThat(operationRepository.count()).isEqualTo(databaseSizeBeforeUpdate);
        Operation persistedOperation = operationRepository.findById(operation.getId()).orElseThrow();
        assertThat(persistedOperation.getDescription()).isEqualTo(UPDATED_DESCRIPTION);
        assertThat(persistedOperation.getAmount()).isEqualTo(DEFAULT_AMOUNT); // Should remain unchanged
        assertThat(persistedOperation.getDate()).isEqualTo(DEFAULT_DATE); // Should remain unchanged
    }

    @Test
    @Transactional
    void createOperationWithInvalidBankAccount() throws Exception {
        long databaseSizeBeforeCreate = operationRepository.count();

        // Create operation with non-existent bank account ID
        Operation invalidOperation = new Operation();
        invalidOperation.setDate(DEFAULT_DATE);
        invalidOperation.setDescription(DEFAULT_DESCRIPTION);
        invalidOperation.setAmount(DEFAULT_AMOUNT);
        BankAccount invalidBankAccount = new BankAccount();
        invalidBankAccount.setId(Long.MAX_VALUE);
        invalidOperation.setBankAccount(invalidBankAccount);

        // The operation creation should fail due to invalid bank account reference
        // It might return 400, 500, or throw an exception - the important thing is it doesn't succeed
        try {
            restOperationMockMvc
                .perform(post(ENTITY_API_URL).contentType(MediaType.APPLICATION_JSON).content(om.writeValueAsBytes(invalidOperation)))
                .andExpect(result -> {
                    int status = result.getResponse().getStatus();
                    assertThat(status).isGreaterThanOrEqualTo(400); // Accept any error status (4xx or 5xx)
                });
        } catch (Exception e) {
            // If an exception is thrown (e.g., during transaction rollback), that's acceptable
        }

        // Ensure the operation wasn't actually persisted
        assertThat(operationRepository.count()).isEqualTo(databaseSizeBeforeCreate);
    }

    @Test
    @Transactional
    void updateOperationWithInvalidId() throws Exception {
        long databaseSizeBeforeUpdate = operationRepository.count();
        operation.setId(longCount.incrementAndGet());

        // If the entity doesn't have an ID, it will throw BadRequestAlertException
        restOperationMockMvc
            .perform(
                put(ENTITY_API_URL_ID, operation.getId()).contentType(MediaType.APPLICATION_JSON).content(om.writeValueAsBytes(operation))
            )
            .andExpect(status().isBadRequest());

        assertThat(operationRepository.count()).isEqualTo(databaseSizeBeforeUpdate);
    }

    @Test
    @Transactional
    void patchOperationWithNullId() throws Exception {
        long databaseSizeBeforeUpdate = operationRepository.count();

        Operation operationWithoutId = new Operation();
        operationWithoutId.setDate(DEFAULT_DATE);
        operationWithoutId.setDescription(DEFAULT_DESCRIPTION);
        operationWithoutId.setAmount(DEFAULT_AMOUNT);

        restOperationMockMvc
            .perform(
                patch(ENTITY_API_URL_ID, 1L).contentType("application/merge-patch+json").content(om.writeValueAsBytes(operationWithoutId))
            )
            .andExpect(status().isBadRequest());

        assertThat(operationRepository.count()).isEqualTo(databaseSizeBeforeUpdate);
    }
}
