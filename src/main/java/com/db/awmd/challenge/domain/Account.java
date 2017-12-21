package com.db.awmd.challenge.domain;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.hibernate.validator.constraints.NotEmpty;

import javax.validation.constraints.Min;
import javax.validation.constraints.NotNull;
import javax.xml.bind.annotation.XmlTransient;
import java.beans.Transient;
import java.math.BigDecimal;
import java.util.concurrent.locks.ReentrantLock;

@Data
@Slf4j
public class Account {

    @NotNull
    @NotEmpty
    private final String accountId;

    @JsonIgnore
    private ReentrantLock lock = new ReentrantLock();

    @NotNull
    @Min(value = 0, message = "Initial balance must be positive.")
    private BigDecimal balance;

    public Account(String accountId) {
        this.accountId = accountId;
        this.balance = BigDecimal.ZERO;
    }

    @JsonCreator
    public Account(@JsonProperty("accountId") String accountId,
                   @JsonProperty("balance") BigDecimal balance) {
        this.accountId = accountId;
        this.balance = balance;
    }

    /**
     * Deposit money
     *
     * @param money which will be deposit
     * @return deposit
     */
    public BigDecimal depositMoney(BigDecimal money) {
        balance = balance.add(money);
        log.info("Account Id: " + accountId + " was deposit: " + money);
        log.info("Account Id: " + accountId + " now has deposit: " + balance);
        return balance;
    }

    /**
     * Withdraw money
     *
     * @param money which will be withdraw
     * @return deposit
     */
    public BigDecimal withdrawMoney(BigDecimal money) {
        balance = balance.subtract(money);
        log.info("Account Id: " + accountId + " was withdraw: " + money);
        log.info("Account Id: " + accountId + " now has deposit: " + balance);
        return balance;
    }


}
