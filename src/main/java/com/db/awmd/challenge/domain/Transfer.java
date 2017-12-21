package com.db.awmd.challenge.domain;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import lombok.ToString;
import org.hibernate.validator.constraints.NotEmpty;

import javax.validation.constraints.Min;
import javax.validation.constraints.NotNull;
import java.math.BigDecimal;

@Data
@ToString
public class Transfer {

    @NotNull
    @NotEmpty
    private String accountFromId;

    @NotNull
    @NotEmpty
    private String accountToId;

    /**
     * The amount to transfer should always be a positive number.
     * It should not be possible for an account to end up with negative balance (we do not support overdrafts!)
     */
    @NotNull
    @Min(value = 0, message = "The amount to transfer should be a positive number.")
    private BigDecimal amountToTransfer;


    @JsonCreator
    public Transfer(@JsonProperty("accountFromId") String accountFromId,
                    @JsonProperty("accountToId") String accountToId,
                    @JsonProperty("amountToTransfer") BigDecimal amountToTransfer) {
        this.accountFromId = accountFromId;
        this.accountToId = accountToId;
        this.amountToTransfer = amountToTransfer;
    }
}
