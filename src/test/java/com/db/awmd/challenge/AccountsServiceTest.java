package com.db.awmd.challenge;

import com.db.awmd.challenge.domain.Account;
import com.db.awmd.challenge.exception.DuplicateAccountIdException;
import com.db.awmd.challenge.service.AccountsService;
import com.db.awmd.challenge.service.NotificationService;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit4.SpringRunner;

import java.math.BigDecimal;

import static junit.framework.TestCase.assertTrue;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.fail;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyZeroInteractions;

@ActiveProfiles("integration_tests")
@RunWith(SpringRunner.class)
@SpringBootTest
public class AccountsServiceTest {

    private static final String ACC_ID_1 = "Id-1";
    private static final String ACC_ID_2 = "Id-2";
    @Autowired
    private AccountsService accountsService;

    @Autowired
    private NotificationService notificationService;

    @Before
    public void before() {
        accountsService.getAccountsRepository().clearAccounts();
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
        createStandardAccountPair();
        this.accountsService.transfer(ACC_ID_1, ACC_ID_2, BigDecimal.valueOf(10L));

        assertTrue(accountsService.getAccount(ACC_ID_1).getBalance().compareTo(BigDecimal.valueOf(0.10)) == 0);
        assertTrue(accountsService.getAccount(ACC_ID_2).getBalance().compareTo(BigDecimal.valueOf(30.20)) == 0);
    }

    @Test
    public void transferBetweenAccountsAndNotificationChecking() {
        createStandardAccountPair();
        this.accountsService.transfer(ACC_ID_1, ACC_ID_2, BigDecimal.valueOf(10L));

        notificationsServiceChecking(accountsService.getAccount(ACC_ID_1),
                accountsService.getAccount(ACC_ID_2));
    }


    private void createStandardAccountPair() {
        Account account1 = new Account(ACC_ID_1, new BigDecimal("10.10"));
        this.accountsService.createAccount(account1);

        Account account2 = new Account(ACC_ID_2, new BigDecimal("20.20"));
        this.accountsService.createAccount(account2);
    }


    @Test(expected = javax.validation.ValidationException.class)
    public void tryTransferBetweenAccountsMoreThanFirstAccountHave() {
        createStandardAccountPair();
        this.accountsService.transfer(ACC_ID_1, ACC_ID_2, BigDecimal.valueOf(100L));
    }

    @Test
    public void tryTransferMoreThanFirstAccountHave() {
        createStandardAccountPair();
        try {
            this.accountsService.transfer(ACC_ID_1, ACC_ID_2, BigDecimal.valueOf(100L));
        } catch (Exception e) {
            assertTrue(e.getMessage().equals("Error list is: ACCOUNT_FROM_ID_DO_NOT_HAVE_ENOUGH_MONEY"));
        }
    }

    @Test
    public void tryTransferMoreThanFirstAccountHaveAndVerifyNotification() {
        createStandardAccountPair();
        try {
            this.accountsService.transfer(ACC_ID_1, ACC_ID_2, BigDecimal.valueOf(100L));
        } catch (Exception e) {
            assertTrue(e.getMessage().equals("Error list is: ACCOUNT_FROM_ID_DO_NOT_HAVE_ENOUGH_MONEY"));
        }
        verifyZeroInteractions(notificationService);
    }

    private void notificationsServiceChecking(final Account accountFrom, final Account accountTo) {
        verify(notificationService, Mockito.times(1)).notifyAboutTransfer(accountFrom,
                "Account Id: " + accountFrom.getAccountId() + " was withdraw." +
                        "Now it has balance: " + accountFrom.getBalance());
        verify(notificationService, Mockito.times(1)).notifyAboutTransfer(accountTo,
                "Account Id: " + accountTo.getAccountId() + " was deposit." +
                        "Now it has balance: " + accountTo.getBalance());
    }
}
