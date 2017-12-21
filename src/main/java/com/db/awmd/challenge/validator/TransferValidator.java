package com.db.awmd.challenge.validator;

import com.db.awmd.challenge.domain.Account;
import com.db.awmd.challenge.domain.Transfer;
import com.db.awmd.challenge.repository.AccountsRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;
import org.springframework.validation.Errors;
import org.springframework.validation.Validator;

import java.math.BigDecimal;

import static com.db.awmd.challenge.validator.TransferErrorCode.*;

@Component
@Scope("prototype")
@Slf4j
public class TransferValidator implements Validator {

    @Autowired
    private AccountsRepository accountsRepository;


    @Override
    public boolean supports(Class<?> clazz) {
        return Transfer.class.isAssignableFrom(clazz);
    }

    @Override
    public void validate(Object target, Errors errors) {
        Transfer transfer = (Transfer) target;
        if (!argumentValidation(errors, transfer)) return;
        if (!accountsValidation(errors, transfer)) return;

        log.debug("Transfer object was validated successfully");

    }

    protected boolean accountsValidation(Errors errors, Transfer transfer) {
        if (accountsRepository.getAccount(transfer.getAccountFromId()) == null) {
            errors.rejectValue("accountFromId", ACCOUNT_FROM_ID_NOT_FOUND.name());
            return false;
        }
        if (accountsRepository.getAccount(transfer.getAccountFromId()).getBalance().compareTo(BigDecimal.ZERO) == -1) {
            errors.rejectValue("accountFromId", ACCOUNT_FROM_BALANCE_NEGATIVE.name());
            return false;
        }

        if (!isAccountFromHasEnoughMoneyForTransfer(transfer)) {
            errors.rejectValue("accountFromId", ACCOUNT_FROM_ID_DO_NOT_HAVE_ENOUGH_MONEY.name());
        }

        if (accountsRepository.getAccount(transfer.getAccountToId()) == null) {
            errors.rejectValue("accountToId", ACCOUNT_TO_ID_NOT_FOUND.name());
            return false;
        }

        if (accountsRepository.getAccount(transfer.getAccountToId()).getBalance().compareTo(BigDecimal.ZERO) == -1) {
            errors.rejectValue("accountToId", ACCOUNT_TO_BALANCE_NEGATIVE.name());
        }
        return true;
    }

    protected boolean argumentValidation(Errors errors, Transfer transfer) {
        if (transfer.getAmountToTransfer() == null) {
            errors.rejectValue("amountToTransfer", AMOUNT_TO_TRANSFER_IS_NULL.name());
            return false;
        }

        if (transfer.getAmountToTransfer().compareTo(BigDecimal.ZERO) != 1) {
            errors.rejectValue("amountToTransfer", AMOUNT_TO_TRANSFER_IS_NEGATIVE_OR_ZERO.name());
        }

        if (transfer.getAccountFromId() == null) {
            errors.rejectValue("accountFromId", ACCOUNT_FROM_ID_IS_NULL.name());
            return false;
        }
        if (transfer.getAccountToId() == null) {
            errors.rejectValue("accountToId", ACCOUNT_TO_ID_IS_NULL.name());
            return false;
        }
        if (transfer.getAccountFromId().equals(transfer.getAccountToId())) {
            errors.reject(TRANSFER_TO_THE_SAME_ACCOUNT.name());
        }
        return true;
    }

    protected boolean isAccountFromHasEnoughMoneyForTransfer(Transfer transfer) {
        Account accountFrom = accountsRepository.getAccount(transfer.getAccountFromId());
        return accountFrom.getBalance().compareTo(transfer.getAmountToTransfer()) != -1;
    }
}
