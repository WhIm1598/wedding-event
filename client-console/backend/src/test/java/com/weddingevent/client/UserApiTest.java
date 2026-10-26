package com.weddingevent.client;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.weddingevent.common.domain.User;
import com.weddingevent.common.domain.enums.PlanStatus;
import com.weddingevent.common.domain.enums.Role;
import com.weddingevent.common.repository.VendorProfileRepository;
import com.weddingevent.common.repository.WeddingPlanRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;

/** FN-AUTH-03 (role picker) and FN-AUTH-04 (profile) */
class UserApiTest extends ClientIntegrationTest {

    @Autowired
    WeddingPlanRepository plans;

    @Autowired
    VendorProfileRepository vendors;

    @Test
    void choosingCoupleCreatesADraftPlanThatIsNotYetAPlan() throws Exception {
        User u = createUser(null, true);
        String token = token(u);
        mvc.perform(authed(put("/api/v1/users/role"), token).content(json("{'role':'ROLE_COUPLE'}")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.role").value("ROLE_COUPLE"))
                .andExpect(jsonPath("$.data.hasPlan").value(false));
        assertThat(plans.findByOwnerIdAndStatus(u.getId(), PlanStatus.DRAFT)).isPresent();

        mvc.perform(authed(get("/api/v1/client/plans/my-plan"), token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data").doesNotExist());

        // The plan the couple creates later reuses the draft row
        String planId = createPlan(token);
        assertThat(plans.findById(java.util.UUID.fromString(planId)).orElseThrow().getStatus()).isEqualTo(PlanStatus.PLANNING);
        assertThat(plans.findByOwnerIdAndStatus(u.getId(), PlanStatus.DRAFT)).isEmpty();
        mvc.perform(authed(get("/api/v1/auth/me"), token)).andExpect(jsonPath("$.data.hasPlan").value(true));
    }

    @Test
    void choosingVendorCreatesAVendorProfile() throws Exception {
        User u = createUser(null, true);
        mvc.perform(authed(put("/api/v1/users/role"), token(u)).content(json("{'role':'ROLE_VENDOR'}")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.role").value("ROLE_VENDOR"));
        var profile = vendors.findByUserId(u.getId()).orElseThrow();
        assertThat(profile.getBusinessName()).isEqualTo(u.getFullName());
        assertThat(profile.getReviewCount()).isZero();
    }

    @Test
    void roleCanBeChosenOnlyOnce() throws Exception {
        String token = token(createUser(Role.ROLE_COUPLE, true));
        mvc.perform(authed(put("/api/v1/users/role"), token).content(json("{'role':'ROLE_VENDOR'}")))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("ROLE_ALREADY_SET"));
    }

    @Test
    void studioRolesCannotBeSelfAssigned() throws Exception {
        String token = token(createUser(null, true));
        for (String role : new String[] {"ROLE_ADMIN", "ROLE_STAFF", "admin", ""}) {
            mvc.perform(authed(put("/api/v1/users/role"), token).content(json("{'role':'" + role + "'}")))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.errors[0].field").value("role"));
        }
    }

    @Test
    void rolelessUsersCannotUseCoupleOrVendorFeatures() throws Exception {
        String token = token(createUser(null, true));
        mvc.perform(authed(get("/api/v1/client/plans/my-plan"), token))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("FORBIDDEN"));
        mvc.perform(authed(get("/api/v1/client/services"), token)).andExpect(status().isForbidden());
        mvc.perform(authed(post("/api/v1/messages/send"), token).content("{}")).andExpect(status().isForbidden());
    }

    @Test
    void updateProfileSavesNameAndPhone() throws Exception {
        User u = createUser(Role.ROLE_COUPLE, true);
        String phone = uniquePhone();
        mvc.perform(authed(put("/api/v1/users/me"), token(u))
                        .content(json("{'fullName':'Ngọc & Hoàng','email':'" + u.getEmail() + "','phone':'" + phone + "'}")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.fullName").value("Ngọc & Hoàng"))
                .andExpect(jsonPath("$.data.phone").value(phone))
                .andExpect(jsonPath("$.data.isVerified").value(true));

        mvc.perform(authed(put("/api/v1/users/me"), token(u))
                        .content(json("{'fullName':'Ngọc & Hoàng','email':'" + u.getEmail() + "','phone':''}")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.phone").doesNotExist());
    }

    private org.springframework.test.web.servlet.ResultActions verifyOtp(String email, String code) throws Exception {
        return mvc.perform(post("/api/v1/auth/verify-otp").contentType(MediaType.APPLICATION_JSON)
                .content(json("{'email':'" + email + "','otpCode':'" + code + "'}")));
    }

    @Test
    void aNewEmailOnlyReplacesTheOldOneOnceVerified() throws Exception {
        User u = createUser(Role.ROLE_COUPLE, true);
        String token = token(u);
        String newEmail = "moi-" + java.util.UUID.randomUUID() + "@example.com";
        mvc.perform(authed(put("/api/v1/users/me"), token)
                        .content(json("{'fullName':'A','email':'" + newEmail + "'}")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.email").value(u.getEmail()))
                .andExpect(jsonPath("$.data.pendingEmail").value(newEmail))
                .andExpect(jsonPath("$.data.isVerified").value(true));
        String code = otpSender.lastCodeFor(newEmail);
        assertThat(code).matches("\\d{4}");

        // A code proves only the address it was sent to
        verifyOtp(u.getEmail(), code).andExpect(status().isBadRequest());
        verifyOtp(newEmail, code).andExpect(status().isOk());
        mvc.perform(authed(get("/api/v1/auth/me"), token))
                .andExpect(jsonPath("$.data.email").value(newEmail))
                .andExpect(jsonPath("$.data.pendingEmail").doesNotExist())
                .andExpect(jsonPath("$.data.isVerified").value(true));
    }

    @Test
    void anUnverifiedEmailChangeCannotClaimSomeoneElsesAddress() throws Exception {
        User squatter = createUser(Role.ROLE_COUPLE, true);
        String victim = "victim-" + java.util.UUID.randomUUID() + "@gmail.com";
        mvc.perform(authed(put("/api/v1/users/me"), token(squatter))
                        .content(json("{'fullName':'A','email':'" + victim + "'}")))
                .andExpect(status().isOk());

        // The real owner signs in with Google and gets an account of their own, not the squatter's
        String owner = body(mvc.perform(post("/api/v1/auth/google").contentType(MediaType.APPLICATION_JSON)
                        .content(json("{'idToken':'google|" + victim + "|Chủ Thật|true'}")))
                .andExpect(status().isOk()));
        assertThat(read(owner, "$.data.user.id")).isNotEqualTo(squatter.getId().toString());

        // The squatter's pending change can no longer complete, even with the code from the owner's mailbox
        verifyOtp(victim, otpSender.lastCodeFor(victim))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("EMAIL_EXISTS"));
        assertThat(users.findById(squatter.getId()).orElseThrow().getEmail()).isEqualTo(squatter.getEmail());
    }

    @Test
    void profileValidation() throws Exception {
        User u = createUser(Role.ROLE_COUPLE, true);
        User other = createUser(Role.ROLE_COUPLE, true);
        String token = token(u);
        mvc.perform(authed(put("/api/v1/users/me"), token)
                        .content(json("{'fullName':'A','email':'" + other.getEmail().toUpperCase() + "'}")))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("EMAIL_EXISTS"));
        mvc.perform(authed(put("/api/v1/users/me"), token)
                        .content(json("{'fullName':'','email':'bad','phone':'0123'}")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.length()").value(3));
        mvc.perform(authed(put("/api/v1/users/me"), token)
                        .content(json("{'fullName':'" + "x".repeat(101) + "','email':'" + u.getEmail() + "'}")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors[0].field").value("fullName"));
    }
}
