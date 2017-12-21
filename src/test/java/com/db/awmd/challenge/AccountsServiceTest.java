package com.db.awmd.challenge;

import com.db.awmd.challenge.domain.Account;
import com.db.awmd.challenge.exception.DuplicateAccountIdException;
import com.db.awmd.challenge.service.AccountsService;
import com.db.awmd.challenge.service.NotificationService;
import org.junit.After;
import org.junit.Before;
import org.junit.FixMethodOrder;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.MethodSorters;
import org.mockito.Mockito;
import org.omg.CORBA.Any;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Scope;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit4.SpringRunner;

import java.math.BigDecimal;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import static junit.framework.TestCase.assertTrue;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.fail;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyZeroInteractions;

@ActiveProfiles("integration_tests")
@RunWith(SpringRunner.class)
@SpringBootTest
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class AccountsServiceTest {

    private Lock sequential = new ReentrantLock();

    private static final String ACC_ID_1 = "Id-1";
    private static final String ACC_ID_2 = "Id-2";
    @Autowired
    private AccountsService accountsService;

    @Autowired
    private NotificationService notificationService;

    @Before
    public void before() {

    }

    @Before
    public void setUp() throws Exception {
        sequential.lock();
        createStandardAccountPair();
        verify(notificationService, Mockito.atLeast(0)).notifyAboutTransfer(any(Account.class), any(String.class));
    }

    @After
    public void tearDown() throws Exception {
        accountsService.getAccountsRepository().clearAccounts();
        sequential.unlock();
    }


    @Test
    public void addAccount() throws Exception {
        Account account = new Account("Id-123");
        account.setBalance(new BigDecimal(1000));
        this.accountsService.createAccount(account);

        assertThat(this.accountsService.getAccount("Id-123")).isEqualTo(account);
    }

    @Test
    public void addAccount_failsOnDuplicateId() throws Exception {
        String uniqueId = "Id-" + System.currentTimeMillis();
        Account account = new Account(uniqueId);
        this.accountsService.createAccount(account);

        try {
            this.accountsService.createAccount(account);
            fail("Should have failed when adding duplicate account");
        } catch (DuplicateAccountIdException ex) {
            assertThat(ex.getMessage()).isEqualTo("Account id " + uniqueId + " already exists!");
        }
    }

    @Test
    public void transferBetweenAccounts2() {
        String account1Id = "Id-" + "12345";
        Account account1 = new Account(account1Id, new BigDecimal("100.45"));
        this.accountsService.createAccount(account1);

        String account2Id = "Id-" + "54321";
        Account account2 = new Account(account2Id, new BigDecimal("200.45"));
        this.accountsService.createAccount(account2);

        this.accountsService.transfer(account1Id, account2Id, BigDecimal.valueOf(100L));

        Account account1AfterTransfer = this.accountsService.getAccount(account1Id);
        Account account2AfterTransfer = this.accountsService.getAccount(account2Id);

        assertTrue(account1AfterTransfer.getBalance().compareTo(BigDecimal.valueOf(0.45)) == 0);
        assertTrue(account2AfterTransfer.getBalance().compareTo(BigDecimal.valueOf(300.45)) == 0);
    }

    @Test
    public void transferBetweenAccounts1() {
        this.accountsService.transfer(ACC_ID_1, ACC_ID_2, BigDecimal.valueOf(10L));

        assertTrue(accountsService.getAccount(ACC_ID_1).getBalance().compareTo(BigDecimal.valueOf(0.10)) == 0);
        assertTrue(accountsService.getAccount(ACC_ID_2).getBalance().compareTo(BigDecimal.valueOf(30.20)) == 0);
    }

    @Test
    public void transferBetweenAccountsAndNotificationChecking() {
        accountsService.transfer(ACC_ID_1, ACC_ID_2, BigDecimal.valueOf(10L));

        verify(notificationService, Mockito.times(1)).notifyAboutTransfer(accountsService.getAccount(ACC_ID_1),
                "Account Id: Id-1 was withdraw. Now it has balance: 0.10");
        verify(notificationService, Mockito.times(1)).notifyAboutTransfer(accountsService.getAccount(ACC_ID_2),
                "Account Id: Id-2 was deposit. Now it has balance: 30.20");

    }


    private void createStandardAccountPair() {
        Account account1 = new Account(ACC_ID_1, new BigDecimal("10.10"));
        this.accountsService.createAccount(account1);

        Account account2 = new Account(ACC_ID_2, new BigDecimal("20.20"));
        this.accountsService.createAccount(account2);
    }


    @Test(expected = javax.validation.ValidationException.class)
    public void tryTransferBetweenAccountsMoreThanFirstAccountHave() {
        this.accountsService.transfer(ACC_ID_1, ACC_ID_2, BigDecimal.valueOf(100L));
    }

    @Test
    public void tryTransferMoreThanFirstAccountHave() {
        try {
            this.accountsService.transfer(ACC_ID_1, ACC_ID_2, BigDecimal.valueOf(100L));
        } catch (Exception e) {
            assertTrue(e.getMessage().equals("Error list is: ACCOUNT_FROM_ID_DO_NOT_HAVE_ENOUGH_MONEY"));
        }
    }

    @Test
    public void transferMoreMoneyThanFromAccountHaveAndVerifyNotification() {
        try {
            this.accountsService.transfer(ACC_ID_1, ACC_ID_2, BigDecimal.valueOf(100L));
        } catch (Exception e) {
            assertTrue(e.getMessage().equals("Error list is: ACCOUNT_FROM_ID_DO_NOT_HAVE_ENOUGH_MONEY"));
        }
        verifyZeroInteractions(notificationService);
    }


    @Test
    public void tryTransferNegativeValue() {
        try {
            this.accountsService.transfer(ACC_ID_1, ACC_ID_2, BigDecimal.valueOf(-100.00));
        } catch (Exception e) {
            assertTrue(e.getMessage().equals("Error list is: AMOUNT_TO_TRANSFER_IS_NEGATIVE_OR_ZERO"));
        }
        verifyZeroInteractions(notificationService);
    }

    @Test
    public void transferNegativeValueTest() {
        try {
            this.accountsService.transfer(ACC_ID_1, ACC_ID_2, BigDecimal.valueOf(-100.00));
        } catch (Exception e) {
            assertTrue(e.getMessage().equals("Error list is: AMOUNT_TO_TRANSFER_IS_NEGATIVE_OR_ZERO"));
        }
        verifyZeroInteractions(notificationService);
    }


    @Test
    public void transferNullValueTest() {
        try {
            this.accountsService.transfer(ACC_ID_1, ACC_ID_2, null);
        } catch (Exception e) {
            assertTrue(e.getMessage().equals("Error list is: AMOUNT_TO_TRANSFER_IS_NULL"));
        }
        verifyZeroInteractions(notificationService);
    }

    @Test
    public void transferToTheSameAccountTest() {
        try {
            this.accountsService.transfer(ACC_ID_1, ACC_ID_1, BigDecimal.valueOf(10L));
        } catch (Exception e) {
            assertTrue(e.getMessage().equals("Error list is: TRANSFER_TO_THE_SAME_ACCOUNT"));
        }
        verifyZeroInteractions(notificationService);
    }

}
