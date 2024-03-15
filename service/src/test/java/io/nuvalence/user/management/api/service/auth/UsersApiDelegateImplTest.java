package io.nuvalence.user.management.api.service.auth;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.nuvalence.auth.access.AuthorizationHandler;
import io.nuvalence.user.management.api.service.entity.PublicUser;
import io.nuvalence.user.management.api.service.entity.UserEntity;
import io.nuvalence.user.management.api.service.entity.profile.IndividualProfileLink;
import io.nuvalence.user.management.api.service.entity.profile.ProfileAccessLevel;
import io.nuvalence.user.management.api.service.enums.ProfileType;
import io.nuvalence.user.management.api.service.enums.UserType;
import io.nuvalence.user.management.api.service.generated.models.AddressModel;
import io.nuvalence.user.management.api.service.generated.models.IndividualProfileCreateModel;
import io.nuvalence.user.management.api.service.generated.models.UserCreationRequest;
import io.nuvalence.user.management.api.service.service.IndividualProfileLinkService;
import io.nuvalence.user.management.api.service.service.IndividualProfileService;
import io.nuvalence.user.management.api.service.service.UserService;
import org.apache.commons.lang3.StringUtils;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.web.servlet.MockMvc;

import java.util.UUID;

/**
 * This class contains tests for Users API Delegate Implementation.
 */
@ExtendWith(SpringExtension.class)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc(addFilters = false)
@ActiveProfiles("test")
class UsersApiDelegateImplTest {

    @Autowired private MockMvc mockMvc;

    @MockBean private UserService userService;
    @MockBean private IndividualProfileLinkService individualUserLinkService;
    @MockBean private IndividualProfileService individualProfileService;

    @MockBean private AuthorizationHandler authorizationHandler;

    @BeforeEach
    void setup() {
        when(authorizationHandler.isAllowed(any(), (Class<?>) any())).thenReturn(true);
        when(authorizationHandler.isAllowed(any(), (String) any())).thenReturn(true);
        when(authorizationHandler.isAllowedForInstance(any(), any())).thenReturn(true);
        when(authorizationHandler.getAuthFilter(any(), any())).thenReturn(element -> true);
    }

    @ParameterizedTest
    @CsvSource({"agency, false", "public, false", "public, true"})
    void addUser(String userType, boolean sendProfileInfo) throws Exception {
        UserCreationRequest user = createNewUserModel(userType);
        if (sendProfileInfo) {
            IndividualProfileCreateModel profile = getIndividualProfileCreateModel();
            user.setProfile(profile);
        }
        final String postBody = new ObjectMapper().writeValueAsString(user);

        ArgumentCaptor<IndividualProfileLink> profileLinkCaptor =
                ArgumentCaptor.forClass(IndividualProfileLink.class);
        when(individualUserLinkService.saveIndividualUserLink(profileLinkCaptor.capture()))
                .thenReturn(mock(IndividualProfileLink.class));

        ArgumentCaptor<UserEntity> userCaptor = ArgumentCaptor.forClass(UserEntity.class);
        when(userService.createUser(userCaptor.capture()))
                .thenAnswer(
                        i -> {
                            UserEntity userEntity = i.getArgument(0);
                            userEntity.setId(UUID.randomUUID());
                            return userEntity;
                        });

        if (userType.equals("public") && sendProfileInfo) {
            doNothing()
                    .when(individualProfileService)
                    .postAuditEventForIndividualProfileCreated(any());
        }

        mockMvc.perform(
                        post("/api/v1/users")
                                .content(postBody)
                                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk());

        verify(userService).createUser(any());

        UserEntity saveduser = userCaptor.getValue();
        assertTrue(StringUtils.isNotBlank(saveduser.getEmail()));
        if (userType.equals("public")) {
            PublicUser publicUser = (PublicUser) userCaptor.getValue();
            if (sendProfileInfo) {
                Assertions.assertEquals(
                        user.getProfile().getFirstName(),
                        publicUser.getIndividualProfile().getFirstName());
                Assertions.assertEquals(
                        user.getProfile().getMiddleName(),
                        publicUser.getIndividualProfile().getMiddleName());
                Assertions.assertEquals(
                        user.getProfile().getLastName(),
                        publicUser.getIndividualProfile().getLastName());
                Assertions.assertEquals(user.getProfile().getEmail(), publicUser.getEmail());
            } else {
                Assertions.assertNull(publicUser.getIndividualProfile().getFirstName());
                Assertions.assertNull(publicUser.getIndividualProfile().getMiddleName());
                Assertions.assertNull(publicUser.getIndividualProfile().getLastName());
                Assertions.assertEquals(user.getEmail(), publicUser.getEmail());
            }
            verify(individualUserLinkService).saveIndividualUserLink(any());
            Assertions.assertEquals(
                    ProfileType.INDIVIDUAL, profileLinkCaptor.getValue().getProfileType());
            Assertions.assertEquals(
                    ProfileAccessLevel.ADMIN, profileLinkCaptor.getValue().getProfileAccessLevel());
        } else {
            verify(individualUserLinkService, never()).saveIndividualUserLink(any());
            if (userCaptor.getValue() instanceof PublicUser publicUser) {
                Assertions.assertNull(publicUser.getIndividualProfile());
            }
            Assertions.assertEquals(user.getEmail(), saveduser.getEmail());
        }
    }

    @Test
    @WithMockUser
    void deleteUserById() throws Exception {
        UserEntity userEntity = createMockUser();
        // mock void method
        doNothing().when(userService).deleteUser(userEntity.getId());

        mockMvc.perform(delete("/api/v1/users/" + userEntity.getId().toString()))
                .andExpect(status().isOk());
    }

    private UserCreationRequest createNewUserModel(String userType) {
        UserCreationRequest user = new UserCreationRequest();
        user.setIdentityProvider("https://securetoken.google.com/my-project");
        user.setExternalId("SMGjTO5n3sZFVIi5IzpW2pI8vjf1");
        user.firstName("Rusty");
        user.middleName("Popins");
        user.lastName("Smith");
        user.phoneNumber("555-555-5555");
        user.setEmail("Rdawg@gmail.com");
        user.setUserType(userType);
        return user;
    }

    private UserEntity createMockUser() {
        UserEntity user = new PublicUser();
        user.setId(UUID.randomUUID());
        user.setFirstName("Rusty");
        user.setMiddleName("Popins");
        user.setLastName("Smith");
        user.setEmail("Rdawg@gmail.com");
        user.setPhoneNumber("555-555-5555");
        user.setIdentityProvider("https://securetoken.google.com/my-project");
        user.setExternalId("SMGjTO5n3sZFVIi5IzpW2pI8vjf1");
        user.setUserType(UserType.PUBLIC);
        return user;
    }

    private static IndividualProfileCreateModel getIndividualProfileCreateModel() {
        AddressModel addressModel = new AddressModel();
        addressModel.setAddress1("addressLine1");
        addressModel.setAddress2("addressLine2");
        addressModel.setCity("city");
        addressModel.setState("state");
        addressModel.setPostalCode("zipCode");
        addressModel.setCountry("someCountry");
        addressModel.setCounty("someCounty");

        IndividualProfileCreateModel profile = new IndividualProfileCreateModel();
        profile.ssn("123-45-6789");
        profile.primaryAddress(addressModel);
        profile.mailingAddress(addressModel);
        profile.setFirstName("Rusty");
        profile.setMiddleName("Popins");
        profile.setLastName("Smith");
        profile.setEmail("some@email.com");
        profile.phoneNumber("555-555-5555");
        return profile;
    }
}
