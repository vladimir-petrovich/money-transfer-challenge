package com.db.awmd.challenge.service;

import com.db.awmd.challenge.domain.Account;
import com.db.awmd.challenge.domain.Transfer;
import com.db.awmd.challenge.exception.TransferException;
import com.db.awmd.challenge.repository.AccountsRepository;
import com.db.awmd.challenge.validator.TransferInitialValidator;
import com.db.awmd.challenge.validator.TransferValidator;
import lombok.Getter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.validation.BindException;
import org.springframework.validation.Errors;
import org.springframework.validation.ObjectError;
import org.springframework.validation.Validator;

import javax.validation.ValidationException;
import java.math.BigDecimal;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReentrantLock;
import java.util.stream.Collectors;

@Service
public class AccountsService {

    @Getter
    private final AccountsRepository accountsRepository;
    @Autowired
    private TransferValidator transferValidator;
    @Autowired
    private TransferInitialValidator transferInitialValidator;
    @Autowired
    private NotificationService notificationService;

    @Autowired
    public AccountsService(AccountsRepository accountsRepository) {
        this.accountsRepository = accountsRepository;
    }

    public void createAccount(Account account) {
        this.accountsRepository.createAccount(account);
    }

    public Account getAccount(String accountId) {
        return this.accountsRepository.getAccount(accountId);
    }

    public void transfer(String accountFromId, String accountToId, BigDecimal amountToTransfer) {
        Transfer transfer = new Transfer(accountFromId, accountToId, amountToTransfer);
        validateTransfer(transfer, transferInitialValidator);

        final int LOCK_TIMEOUT = 100;
        final ReentrantLock accountFromLock = accountsRepository.getAccount(transfer.getAccountFromId()).getLock();
        final ReentrantLock accountToLock = accountsRepository.getAccount(transfer.getAccountToId()).getLock();

        try {
            Account accountFrom = null;
            Account accountTo = null;
            if (accountFromLock.tryLock(LOCK_TIMEOUT, TimeUnit.MILLISECONDS)) {
                try {
                    if (accountToLock.tryLock(LOCK_TIMEOUT, TimeUnit.MILLISECONDS)) {
                        try {
                            accountFrom = accountsRepository.getAccount(transfer.getAccountFromId());
                            accountTo = accountsRepository.getAccount(transfer.getAccountToId());

                            validateTransfer(transfer, transferValidator);

                            accountsRepository.updateAccount(accountFrom.getAccountId(),
                                    accountFrom.withdrawMoney(transfer.getAmountToTransfer()));
                            accountsRepository.updateAccount(accountTo.getAccountId(),
                                    accountTo.depositMoney(transfer.getAmountToTransfer()));

                        } finally {
                            accountToLock.unlock();
                        }
                    }
                } finally {
                    accountFromLock.unlock();
                }

                notificationService.notifyAboutTransfer(accountFrom,
                        "Account Id: " + accountFrom.getAccountId() + " was withdraw." +
                                " Now it has balance: " + accountFrom.getBalance());
                notificationService.notifyAboutTransfer(accountTo,
                        "Account Id: " + accountTo.getAccountId() + " was deposit." +
                                " Now it has balance: " + accountTo.getBalance());
            } else {
                throw new TransferException("Money were not transferred");
            }
        } catch (InterruptedException e) {
            throw new TransferException("Money were not transferred because of interruption");
        }
    }

    private void validateTransfer(Transfer transfer, Validator validator) throws ValidationException {
        Errors errors = new BindException(transfer, "transfer");
        transferValidator.validate(transfer, errors);
        if (errors.hasErrors()) {
            List<String> errorList = errors.getAllErrors().stream().map(ObjectError::getCode).collect(Collectors.toList());
            String errorsCommaSeparated = String.join(",", errorList);
            throw new ValidationException("Error list is: " + errorsCommaSeparated);
        }
    }

}
