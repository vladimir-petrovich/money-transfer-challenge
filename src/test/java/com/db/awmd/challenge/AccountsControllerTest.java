package com.db.awmd.challenge;

import com.db.awmd.challenge.domain.Account;
import com.db.awmd.challenge.service.AccountsService;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.context.web.WebAppConfiguration;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.ResultMatcher;
import org.springframework.web.context.WebApplicationContext;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.setup.MockMvcBuilders.webAppContextSetup;

@RunWith(SpringRunner.class)
@SpringBootTest
@WebAppConfiguration
public class AccountsControllerTest {

    private MockMvc mockMvc;

    @Autowired
    private AccountsService accountsService;

    @Autowired
    private WebApplicationContext webApplicationContext;

    private static final String ID_1 = "Id-1";
    private static final String ID_2 = "Id-2";


    @Before
    public void prepareMockMvc() throws Exception {
        this.mockMvc = webAppContextSetup(this.webApplicationContext).build();
        accountsService.getAccountsRepository().clearAccounts();
        createStandardAccountPair();
    }

    @Test
    public void createAccount() throws Exception {
        this.mockMvc.perform(post("/v1/accounts").contentType(MediaType.APPLICATION_JSON)
                .content("{\"accountId\":\"Id-123\",\"balance\":1000}")).andExpect(status().isCreated());

        Account account = accountsService.getAccount("Id-123");
        assertThat(account.getAccountId()).isEqualTo("Id-123");
        assertThat(account.getBalance()).isEqualByComparingTo("1000");
    }

    @Test
    public void createDuplicateAccount() throws Exception {
        this.mockMvc.perform(post("/v1/accounts").contentType(MediaType.APPLICATION_JSON)
                .content("{\"accountId\":\"Id-123\",\"balance\":1000}")).andExpect(status().isCreated());

        this.mockMvc.perform(post("/v1/accounts").contentType(MediaType.APPLICATION_JSON)
                .content("{\"accountId\":\"Id-123\",\"balance\":1000}")).andExpect(status().isBadRequest());
    }

    @Test
    public void createAccountNoAccountId() throws Exception {
        this.mockMvc.perform(post("/v1/accounts").contentType(MediaType.APPLICATION_JSON)
                .content("{\"balance\":1000}")).andExpect(status().isBadRequest());
    }

    @Test
    public void createAccountNoBalance() throws Exception {
        this.mockMvc.perform(post("/v1/accounts").contentType(MediaType.APPLICATION_JSON)
                .content("{\"accountId\":\"Id-123\"}")).andExpect(status().isBadRequest());
    }

    @Test
    public void createAccountNoBody() throws Exception {
        this.mockMvc.perform(post("/v1/accounts").contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest());
    }

    @Test
    public void createAccountNegativeBalance() throws Exception {
        this.mockMvc.perform(post("/v1/accounts").contentType(MediaType.APPLICATION_JSON)
                .content("{\"accountId\":\"Id-123\",\"balance\":-1000}")).andExpect(status().isBadRequest());
    }

    @Test
    public void createAccountEmptyAccountId() throws Exception {
        this.mockMvc.perform(post("/v1/accounts").contentType(MediaType.APPLICATION_JSON)
                .content("{\"accountId\":\"\",\"balance\":1000}")).andExpect(status().isBadRequest());
    }

    @Test
    public void getAccount() throws Exception {
        String uniqueAccountId = "Id-" + System.currentTimeMillis();
        Account account = new Account(uniqueAccountId, new BigDecimal("123.45"));
        this.accountsService.createAccount(account);
        this.mockMvc.perform(get("/v1/accounts/" + uniqueAccountId))
                .andExpect(status().isOk())
                .andExpect(
                        content().string("{\"accountId\":\"" + uniqueAccountId + "\",\"balance\":123.45}"));
    }

    @Test
    public void transferBetweenAccounts() throws Exception {
        transfer(200);
        assertThat(accountsService.getAccount(ID_1).getBalance()).isEqualByComparingTo("800");
        assertThat(accountsService.getAccount(ID_2).getBalance()).isEqualByComparingTo("1200");
    }

    private void transfer(double amountToTransfer) throws Exception {
        this.mockMvc.perform(put("/v1/accounts/transfer").contentType(MediaType.APPLICATION_JSON)
                .content("{\"accountFromId\":\"" + ID_1 + "\"," +
                        " \"accountToId\":\"" + ID_2 + "\"," +
                        "\"amountToTransfer\":" + amountToTransfer + "}")).andExpect(status().isOk());
    }

    private ResultActions getTransferStatus(double amountToTransfer) throws Exception {
        return this.mockMvc.perform(put("/v1/accounts/transfer").contentType(MediaType.APPLICATION_JSON)
                .content("{\"accountFromId\":\"" + ID_1 + "\"," +
                        " \"accountToId\":\"" + ID_2 + "\"," +
                        "\"amountToTransfer\":" + amountToTransfer + "}"));
    }

    private void createStandardAccountPair() throws Exception {
            mockMvc.perform(post("/v1/accounts").contentType(MediaType.APPLICATION_JSON)
                    .content("{\"accountId\":\"" + ID_1 + "\",\"balance\":1000}")).andExpect(status().isCreated());
            mockMvc.perform(post("/v1/accounts").contentType(MediaType.APPLICATION_JSON)
                    .content("{\"accountId\":\"" + ID_2 + "\",\"balance\":1000}")).andExpect(status().isCreated());
    }

    @Test
    public void transferBetweenAccountsZeroValue() throws Exception {
        ResultActions resultActions =  getTransferStatus(0);
        resultActions.andExpect(status().isBadRequest());
    }

    @Test
    public void wrongRequestBody() throws Exception {
        ResultActions resultActions =  getTransferStatus(0);
        this.mockMvc.perform(put("/v1/accounts/transfer").contentType(MediaType.APPLICATION_JSON)
                .content("{\"accountFromId\":\" }")).andExpect(status().isBadRequest());
    }

}
