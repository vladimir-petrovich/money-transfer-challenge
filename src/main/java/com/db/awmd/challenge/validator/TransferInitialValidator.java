package com.db.awmd.challenge.validator;

import com.db.awmd.challenge.domain.Transfer;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;
import org.springframework.validation.Errors;

@Component
@Scope("prototype")
@Slf4j
public class TransferInitialValidator extends TransferValidator {
    protected boolean accountsValidation(Errors errors, Transfer transfer) {
        //We do not validate the account on the initial state
        return true;
    }
}
