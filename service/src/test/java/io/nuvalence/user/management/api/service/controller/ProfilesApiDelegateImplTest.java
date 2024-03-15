package io.nuvalence.user.management.api.service.controller;

import static org.hamcrest.Matchers.hasSize;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.Appender;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.nuvalence.auth.access.AuthorizationHandler;
import io.nuvalence.auth.util.SecurityContextUtility;
import io.nuvalence.user.management.api.service.config.SpringConfig;
import io.nuvalence.user.management.api.service.config.exception.ConflictException;
import io.nuvalence.user.management.api.service.entity.PublicUser;
import io.nuvalence.user.management.api.service.entity.profile.Address;
import io.nuvalence.user.management.api.service.entity.profile.EmployerProfile;
import io.nuvalence.user.management.api.service.entity.profile.EmployerProfileLink;
import io.nuvalence.user.management.api.service.entity.profile.IndividualProfile;
import io.nuvalence.user.management.api.service.entity.profile.IndividualProfileLink;
import io.nuvalence.user.management.api.service.entity.profile.ProfileAccessLevel;
import io.nuvalence.user.management.api.service.entity.profile.ProfileInvitation;
import io.nuvalence.user.management.api.service.enums.ProfileType;
import io.nuvalence.user.management.api.service.enums.UserType;
import io.nuvalence.user.management.api.service.generated.models.AddressModel;
import io.nuvalence.user.management.api.service.generated.models.EmployerProfileCreateModel;
import io.nuvalence.user.management.api.service.generated.models.EmployerProfileLinkRequestModel;
import io.nuvalence.user.management.api.service.generated.models.EmployerProfileUpdateModel;
import io.nuvalence.user.management.api.service.generated.models.IndividualProfileCreateModel;
import io.nuvalence.user.management.api.service.generated.models.IndividualProfileLinkUpdateModel;
import io.nuvalence.user.management.api.service.generated.models.IndividualProfileUpdateModel;
import io.nuvalence.user.management.api.service.generated.models.ProfileInvitationRequestModel;
import io.nuvalence.user.management.api.service.models.EmployerFilters;
import io.nuvalence.user.management.api.service.models.EmployerProfileLinkFilters;
import io.nuvalence.user.management.api.service.models.ProfileInvitationFilters;
import io.nuvalence.user.management.api.service.models.auditevents.AuditActivityType;
import io.nuvalence.user.management.api.service.repository.EmployerProfileRepository;
import io.nuvalence.user.management.api.service.repository.IndividualProfileRepository;
import io.nuvalence.user.management.api.service.service.CommonProfileService;
import io.nuvalence.user.management.api.service.service.EmployerProfileLinkService;
import io.nuvalence.user.management.api.service.service.EmployerProfileService;
import io.nuvalence.user.management.api.service.service.IndividualProfileLinkService;
import io.nuvalence.user.management.api.service.service.IndividualProfileService;
import io.nuvalence.user.management.api.service.service.ProfileInvitationService;
import io.nuvalence.user.management.api.service.service.UserService;
import io.nuvalence.user.management.api.service.util.UserUtility;
import jakarta.ws.rs.core.MediaType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.mockito.ArgumentCaptor;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.test.web.servlet.result.MockMvcResultMatchers;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc(addFilters = false)
@ActiveProfiles("test")
@WithMockUser(authorities = {"um:admin"})
@SuppressWarnings("PMD.AvoidDuplicateLiterals")
class ProfilesApiDelegateImplTest {

    @Autowired private MockMvc mockMvc;

    @MockBean private AuthorizationHandler authorizationHandler;

    @MockBean private EmployerProfileRepository employerProfileRepository;

    @MockBean private EmployerProfileService employerProfileService;

    @MockBean private IndividualProfileRepository individualProfileRepository;

    @MockBean private IndividualProfileService individualProfileService;

    @MockBean private Appender<ILoggingEvent> mockAppender;

    @MockBean private IndividualProfileLinkService individualProfileLinkService;

    @MockBean private UserService userService;

    @MockBean private EmployerProfileLinkService employerProfileLinkService;

    @MockBean private CommonProfileService commonProfileService;

    @MockBean private ProfileInvitationService invitationService;

    private ObjectMapper objectMapper;

    private final UUID userId = UUID.randomUUID();
    private static final String USER_EMAIL = "a@b.c";

    @BeforeEach
    void setup() {
        when(authorizationHandler.isAllowed(any(), (Class<?>) any())).thenReturn(true);
        when(authorizationHandler.isAllowed(any(), (String) any())).thenReturn(true);
        when(authorizationHandler.isAllowedForInstance(any(), any())).thenReturn(true);
        when(authorizationHandler.getAuthFilter(any(), any())).thenReturn(element -> true);
        when(commonProfileService.getProfileById(any()))
                .thenReturn(Optional.ofNullable(IndividualProfile.builder().build()));

        Logger logger = (Logger) LoggerFactory.getLogger(ProfilesApiDelegateImpl.class);
        logger.addAppender(mockAppender);

        this.objectMapper = SpringConfig.getMapper();
    }

    @Test
    void getEmployerProfile_Success() throws Exception {
        EmployerProfile employer = createEmployer();
        UUID profileId = employer.getId();

        when(employerProfileService.getEmployerById(profileId)).thenReturn(Optional.of(employer));

        mockMvc.perform(get("/api/v1/profiles/employers/" + profileId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(profileId.toString()))
                .andExpect(jsonPath("$.fein").value(employer.getFein()))
                .andExpect(jsonPath("$.legalName").value(employer.getLegalName()))
                .andExpect(jsonPath("$.otherNames", hasSize(1)))
                .andExpect(jsonPath("$.otherNames[0]").value(employer.getOtherNames().get(0)))
                .andExpect(jsonPath("$.type").value(employer.getType()))
                .andExpect(jsonPath("$.industry").value(employer.getIndustry()))
                .andExpect(jsonPath("$.summaryOfBusiness").value(employer.getSummaryOfBusiness()))
                .andExpect(
                        jsonPath("$.mailingAddress.address1")
                                .value(employer.getMailingAddress().getAddress1()))
                .andExpect(jsonPath("$.locations", hasSize(1)))
                .andExpect(
                        jsonPath("$.locations[0].address1")
                                .value(employer.getLocations().get(0).getAddress1()));
    }

    @Test
    void getEmployerProfile_NotFound() throws Exception {
        EmployerProfile employer = createEmployer();
        UUID profileId = employer.getId();
        when(employerProfileService.getEmployerById(profileId)).thenReturn(Optional.empty());

        mockMvc.perform(get("/api/v1/profiles/employers/" + profileId))
                .andExpect(status().isNotFound())
                .andExpect(content().string("{\"messages\":[\"Employer profile not found\"]}"));
    }

    @Test
    void getEmployerProfiles() throws Exception {
        EmployerProfile employer = createEmployer();
        Page<EmployerProfile> employerPage = new PageImpl<>(Collections.singletonList(employer));
        when(employerProfileService.getEmployersByFilters(any(EmployerFilters.class)))
                .thenReturn(employerPage);

        mockMvc.perform(get("/api/v1/profiles/employers"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items", hasSize(1)))
                .andExpect(jsonPath("$.items[0].id").value(employer.getId().toString()))
                .andExpect(jsonPath("$.items[0].fein").value(employer.getFein()))
                .andExpect(jsonPath("$.items[0].legalName").value(employer.getLegalName()))
                .andExpect(jsonPath("$.items[0].otherNames", hasSize(1)))
                .andExpect(
                        jsonPath("$.items[0].otherNames[0]").value(employer.getOtherNames().get(0)))
                .andExpect(jsonPath("$.items[0].type").value(employer.getType()))
                .andExpect(jsonPath("$.items[0].industry").value(employer.getIndustry()))
                .andExpect(
                        jsonPath("$.items[0].summaryOfBusiness")
                                .value(employer.getSummaryOfBusiness()))
                .andExpect(
                        jsonPath("$.items[0].mailingAddress.address1")
                                .value(employer.getMailingAddress().getAddress1()))
                .andExpect(jsonPath("$.items[0].locations", hasSize(1)))
                .andExpect(
                        jsonPath("$.items[0].locations[0].address1")
                                .value(employer.getLocations().get(0).getAddress1()))
                .andExpect(jsonPath("$.pagingMetadata.totalCount").value(1))
                .andExpect(jsonPath("$.pagingMetadata.pageNumber").value(0));
    }

    @Test
    void getEmployerProfilesForbidden() throws Exception {

        when(authorizationHandler.isAllowed("list", EmployerProfile.class)).thenReturn(false);

        mockMvc.perform(get("/api/v1/profiles/employers")).andExpect(status().isForbidden());
    }

    @Test
    void postEmployerProfile() throws Exception {
        EmployerProfileCreateModel employer = employerProfileCreateModel();

        String requestBodyJson = objectMapper.writeValueAsString(employer);

        when(employerProfileService.saveEmployer(any(EmployerProfile.class)))
                .thenReturn(createEmployer());

        mockMvc.perform(
                        post("/api/v1/profiles/employers")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(requestBodyJson))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.fein").value(employer.getFein()))
                .andExpect(jsonPath("$.legalName").value(employer.getLegalName()))
                .andExpect(jsonPath("$.otherNames", hasSize(1)))
                .andExpect(jsonPath("$.otherNames[0]").value(employer.getOtherNames().get(0)))
                .andExpect(jsonPath("$.type").value(employer.getType()))
                .andExpect(jsonPath("$.industry").value(employer.getIndustry()))
                .andExpect(jsonPath("$.summaryOfBusiness").value(employer.getSummaryOfBusiness()))
                .andExpect(
                        jsonPath("$.mailingAddress.address1")
                                .value(employer.getMailingAddress().getAddress1()))
                .andExpect(jsonPath("$.locations", hasSize(1)))
                .andExpect(
                        jsonPath("$.locations[0].address1")
                                .value(employer.getLocations().get(0).getAddress1()));
    }

    @Test
    void postEmployerProfileForbidden() throws Exception {
        EmployerProfileCreateModel employer = employerProfileCreateModel();

        String requestBodyJson = objectMapper.writeValueAsString(employer);

        when(authorizationHandler.isAllowed("create", EmployerProfile.class)).thenReturn(false);

        mockMvc.perform(
                        post("/api/v1/profiles/employers")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(requestBodyJson))
                .andExpect(status().isForbidden());
    }

    @Test
    void updateEmployerProfile_Success() throws Exception {
        EmployerProfileUpdateModel employer = employerProfileUpdateModel();

        EmployerProfile modifiedEmployer =
                EmployerProfile.builder()
                        .id(UUID.randomUUID())
                        .fein("fein - changed")
                        .legalName("legalName - changed")
                        .otherNames(Collections.singletonList("otherNames - changed"))
                        .type("LLC")
                        .industry("industry - changed")
                        .summaryOfBusiness("summaryOfBusiness - changed")
                        .businessPhone("businessPhone - changed")
                        .mailingAddress(createAddress())
                        .locations(List.of(createAddress()))
                        .build();

        when(employerProfileService.getEmployerById(any(UUID.class)))
                .thenReturn(Optional.of(createEmployer()))
                .thenReturn(Optional.of(modifiedEmployer));

        when(employerProfileService.saveEmployer(any(EmployerProfile.class)))
                .thenReturn(modifiedEmployer);

        String requestBodyJson = objectMapper.writeValueAsString(employer);

        try (MockedStatic<SecurityContextUtility> mock =
                Mockito.mockStatic(SecurityContextUtility.class)) {

            mock.when(SecurityContextUtility::getAuthenticatedUserId)
                    .thenReturn(UUID.randomUUID().toString());

            mockMvc.perform(
                            put("/api/v1/profiles/employers/" + modifiedEmployer.getId())
                                    .contentType(MediaType.APPLICATION_JSON)
                                    .content(requestBodyJson))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.fein").value(employer.getFein()))
                    .andExpect(jsonPath("$.legalName").value(employer.getLegalName()))
                    .andExpect(jsonPath("$.otherNames", hasSize(1)))
                    .andExpect(jsonPath("$.otherNames[0]").value(employer.getOtherNames().get(0)))
                    .andExpect(jsonPath("$.type").value(employer.getType()))
                    .andExpect(jsonPath("$.industry").value(employer.getIndustry()))
                    .andExpect(
                            jsonPath("$.summaryOfBusiness").value(employer.getSummaryOfBusiness()))
                    .andExpect(
                            jsonPath("$.mailingAddress.address1")
                                    .value(employer.getMailingAddress().getAddress1()))
                    .andExpect(jsonPath("$.locations", hasSize(1)))
                    .andExpect(
                            jsonPath("$.locations[0].address1")
                                    .value(employer.getLocations().get(0).getAddress1()));
        }
    }

    @Test
    void updateEmployerProfile_Forbidden() throws Exception {
        EmployerProfileUpdateModel employer = employerProfileUpdateModel();
        UUID profileId = UUID.randomUUID();

        String requestBodyJson = objectMapper.writeValueAsString(employer);

        when(employerProfileService.getEmployerById(profileId))
                .thenReturn(Optional.of(createEmployer()));
        when(authorizationHandler.isAllowedForInstance(eq("update"), any())).thenReturn(false);

        mockMvc.perform(
                        put("/api/v1/profiles/employers/" + profileId)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(requestBodyJson))
                .andExpect(status().isForbidden());
    }

    @Test
    void updateEmployerProfile_NotFound() throws Exception {
        EmployerProfileUpdateModel employer = employerProfileUpdateModel();

        String requestBodyJson = objectMapper.writeValueAsString(employer);

        when(employerProfileRepository.findById(any(UUID.class))).thenReturn(Optional.empty());

        mockMvc.perform(
                        put("/api/v1/profiles/employers/" + UUID.randomUUID())
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(requestBodyJson))
                .andExpect(status().isNotFound())
                .andExpect(content().string("{\"messages\":[\"Employer profile not found\"]}"));
    }

    @Test
    void updateEmployerProfile_NotFound_Access() throws Exception {
        EmployerProfileUpdateModel employer = employerProfileUpdateModel();

        String requestBodyJson = objectMapper.writeValueAsString(employer);

        when(employerProfileRepository.findById(any(UUID.class)))
                .thenReturn(Optional.of(EmployerProfile.builder().build()));

        when(authorizationHandler.isAllowedForInstance(eq("view"), any())).thenReturn(false);

        mockMvc.perform(
                        put("/api/v1/profiles/employers/" + UUID.randomUUID())
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(requestBodyJson))
                .andExpect(status().isNotFound())
                .andExpect(content().string("{\"messages\":[\"Employer profile not found\"]}"));
    }

    @Test
    void getIndividualProfile_Success() throws Exception {
        IndividualProfile individual = createIndividual();
        UUID profileId = individual.getId();

        when(individualProfileService.getIndividualById(profileId))
                .thenReturn(Optional.of(individual));

        mockMvc.perform(get("/api/v1/profiles/individuals/" + profileId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(profileId.toString()))
                .andExpect(jsonPath("$.ssn").value(individual.getSsn()))
                .andExpect(
                        jsonPath("$.primaryAddress.address1")
                                .value(individual.getPrimaryAddress().getAddress1()))
                .andExpect(
                        jsonPath("$.mailingAddress.address1")
                                .value(individual.getMailingAddress().getAddress1()));
    }

    @Test
    void getIndividualProfile_NotFound() throws Exception {
        IndividualProfile individual = createIndividual();
        UUID profileId = individual.getId();
        when(individualProfileRepository.findById(profileId)).thenReturn(Optional.empty());

        mockMvc.perform(get("/api/v1/profiles/individuals/" + profileId))
                .andExpect(status().isNotFound())
                .andExpect(content().string("{\"messages\":[\"Individual profile not found\"]}"));
    }

    @Test
    void postIndividualProfile() throws Exception {
        IndividualProfileCreateModel individual = individualProfileCreateModel();

        String requestBodyJson = objectMapper.writeValueAsString(individual);

        when(individualProfileRepository.save(any(IndividualProfile.class)))
                .thenReturn(createIndividual());
        when(individualProfileService.saveIndividual(any(IndividualProfile.class)))
                .thenReturn(createIndividual());

        mockMvc.perform(
                        post("/api/v1/profiles/individuals")
                                .contentType(org.springframework.http.MediaType.APPLICATION_JSON)
                                .content(requestBodyJson))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.ssn").value(individual.getSsn()))
                .andExpect(
                        jsonPath("$.primaryAddress.address1")
                                .value(individual.getPrimaryAddress().getAddress1()))
                .andExpect(
                        jsonPath("$.mailingAddress.address1")
                                .value(individual.getMailingAddress().getAddress1()));
    }

    @Test
    void postIndividualProfileUnAuthorize() throws Exception {
        IndividualProfileCreateModel individual = individualProfileCreateModel();

        String requestBodyJson = objectMapper.writeValueAsString(individual);

        when(authorizationHandler.isAllowed("create", IndividualProfile.class)).thenReturn(false);

        mockMvc.perform(
                        post("/api/v1/profiles/individuals")
                                .contentType(org.springframework.http.MediaType.APPLICATION_JSON)
                                .content(requestBodyJson))
                .andExpect(status().isForbidden());
    }

    @Test
    void updateIndividualProfile_Success() throws Exception {
        IndividualProfileUpdateModel individual = individualProfileUpdateModel();

        IndividualProfile modifiedIndividual =
                IndividualProfile.builder()
                        .id(UUID.randomUUID())
                        .ssn("ssn2")
                        .primaryAddress(createAddress())
                        .mailingAddress(createAddress())
                        .build();

        when(individualProfileService.getIndividualById(any(UUID.class)))
                .thenReturn(Optional.of(createIndividual()))
                .thenReturn(Optional.of(modifiedIndividual));

        when(individualProfileRepository.save(any(IndividualProfile.class)))
                .thenReturn(modifiedIndividual);

        String requestBodyJson = objectMapper.writeValueAsString(individual);

        try (MockedStatic<SecurityContextUtility> mock =
                Mockito.mockStatic(SecurityContextUtility.class)) {

            mock.when(SecurityContextUtility::getAuthenticatedUserId)
                    .thenReturn(UUID.randomUUID().toString());

            mockMvc.perform(
                            put("/api/v1/profiles/individuals/" + modifiedIndividual.getId())
                                    .contentType(
                                            org.springframework.http.MediaType.APPLICATION_JSON)
                                    .content(requestBodyJson))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.ssn").value(modifiedIndividual.getSsn()))
                    .andExpect(
                            jsonPath("$.primaryAddress.address1")
                                    .value(modifiedIndividual.getPrimaryAddress().getAddress1()))
                    .andExpect(
                            jsonPath("$.mailingAddress.address1")
                                    .value(modifiedIndividual.getMailingAddress().getAddress1()));
        }
    }

    @Test
    void updateIndividualProfile_NotFound() throws Exception {
        IndividualProfileUpdateModel individual = individualProfileUpdateModel();

        String requestBodyJson = objectMapper.writeValueAsString(individual);

        when(individualProfileRepository.findById(any(UUID.class))).thenReturn(Optional.empty());

        mockMvc.perform(
                        put("/api/v1/profiles/individuals/" + UUID.randomUUID())
                                .contentType(org.springframework.http.MediaType.APPLICATION_JSON)
                                .content(requestBodyJson))
                .andExpect(status().isNotFound())
                .andExpect(content().string("{\"messages\":[\"Individual profile not found\"]}"));
    }

    @Test
    void updateIndividualProfile_NotFound_Access() throws Exception {
        IndividualProfileUpdateModel individual = individualProfileUpdateModel();
        UUID profileId = UUID.randomUUID();

        String requestBodyJson = objectMapper.writeValueAsString(individual);

        when(individualProfileRepository.findById(any(UUID.class)))
                .thenReturn(Optional.of(IndividualProfile.builder().build()));
        when(authorizationHandler.isAllowedForInstance(eq("view"), any())).thenReturn(false);

        mockMvc.perform(
                        put("/api/v1/profiles/individuals/" + profileId)
                                .contentType(org.springframework.http.MediaType.APPLICATION_JSON)
                                .content(requestBodyJson))
                .andExpect(status().isNotFound())
                .andExpect(content().string("{\"messages\":[\"Individual profile not found\"]}"));
    }

    @Test
    void updateIndividualProfile_UnAuthorize() throws Exception {
        IndividualProfileUpdateModel individual = individualProfileUpdateModel();
        UUID profileId = UUID.randomUUID();

        String requestBodyJson = objectMapper.writeValueAsString(individual);

        when(individualProfileService.getIndividualById(profileId))
                .thenReturn(Optional.of(createIndividual()));
        when(authorizationHandler.isAllowedForInstance(eq("update"), any())).thenReturn(false);

        mockMvc.perform(
                        put("/api/v1/profiles/individuals/" + profileId)
                                .contentType(org.springframework.http.MediaType.APPLICATION_JSON)
                                .content(requestBodyJson))
                .andExpect(status().isForbidden());
    }

    @Test
    void getIndividualProfiles() throws Exception {
        IndividualProfile individual = createIndividual();
        Page<IndividualProfile> individualPage =
                new PageImpl<>(Collections.singletonList(individual));

        when(individualProfileService.getIndividualsByFilters(any())).thenReturn(individualPage);

        mockMvc.perform(get("/api/v1/profiles/individuals"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items", hasSize(1)))
                .andExpect(jsonPath("$.items[0].id").value(individual.getId().toString()))
                .andExpect(jsonPath("$.items[0].ssn").value(individual.getSsn()))
                .andExpect(
                        jsonPath("$.items[0].primaryAddress.address1")
                                .value(individual.getPrimaryAddress().getAddress1()))
                .andExpect(
                        jsonPath("$.items[0].mailingAddress.address1")
                                .value(individual.getMailingAddress().getAddress1()))
                .andExpect(jsonPath("$.pagingMetadata.totalCount").value(1))
                .andExpect(jsonPath("$.pagingMetadata.pageNumber").value(0));
    }

    @Test
    void getIndividualProfilesUnAuthorized() throws Exception {

        when(authorizationHandler.isAllowed("list", IndividualProfile.class)).thenReturn(false);

        mockMvc.perform(get("/api/v1/profiles/individuals")).andExpect(status().isForbidden());
    }

    @Test
    void deleteIndividualProfileLink_Success() throws Exception {
        // Arrange
        UUID profileId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        IndividualProfileLink userLink = new IndividualProfileLink();
        userLink.setId(UUID.randomUUID());

        when(authorizationHandler.isAllowed("delete", IndividualProfileLink.class))
                .thenReturn(true);
        when(individualProfileService.getIndividualById(profileId))
                .thenReturn(Optional.of(createIndividual()));
        when(individualProfileLinkService.getIndividualUserLinkByProfileAndUserId(
                        profileId, userId))
                .thenReturn(Optional.of(userLink));

        // Act & Assert
        mockMvc.perform(
                        delete(
                                "/api/v1/profiles/individuals/{profileId}/links/{userId}",
                                profileId,
                                userId))
                .andExpect(status().isNoContent());
        verify(individualProfileLinkService).deleteIndividualUserLink(userLink);
    }

    @Test
    void deleteIndividualProfileLink_Forbidden() throws Exception {
        try (MockedStatic<UserUtility> mock = Mockito.mockStatic(UserUtility.class)) {
            mock.when(UserUtility::getAuthenticatedUserType).thenReturn(UserType.PUBLIC.getValue());
            when(authorizationHandler.isAllowedForInstance(eq("delete-link"), any()))
                    .thenReturn(false);

            UUID profileId = UUID.randomUUID();
            UUID userId = UUID.randomUUID();
            when(authorizationHandler.isAllowed("delete", IndividualProfileLink.class))
                    .thenReturn(false);
            when(individualProfileLinkService.getIndividualUserLinkByProfileAndUserId(
                            profileId, userId))
                    .thenReturn(Optional.empty());
            when(employerProfileLinkService.getEmployerUserLink(profileId, userId))
                    .thenReturn(Optional.empty());
            mockMvc.perform(
                            delete(
                                    "/api/v1/profiles/individuals/{profileId}/links/{userId}",
                                    profileId,
                                    userId))
                    .andExpect(status().isForbidden());
        }
    }

    @Test
    void deleteIndividualProfileLink_IndividualNotFound() throws Exception {
        UUID profileId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();

        when(individualProfileService.getIndividualById(profileId)).thenReturn(Optional.empty());

        mockMvc.perform(
                        delete(
                                "/api/v1/profiles/individuals/{profileId}/links/{userId}",
                                profileId,
                                userId))
                .andExpect(status().isNotFound());
    }

    @Test
    void deleteIndividualProfileLink_UserLinkNotFound() throws Exception {
        UUID profileId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();

        when(individualProfileService.getIndividualById(profileId))
                .thenReturn(Optional.of(new IndividualProfile()));
        when(individualProfileLinkService.getIndividualUserLinkByProfileAndUserId(
                        profileId, userId))
                .thenReturn(Optional.empty());

        mockMvc.perform(
                        delete(
                                "/api/v1/profiles/individuals/{profileId}/links/{userId}",
                                profileId,
                                userId))
                .andExpect(status().isNotFound());
    }

    @Test
    void testUpdateIndividualProfileLink() throws Exception {
        UUID profileId = UUID.randomUUID();
        IndividualProfileLinkUpdateModel updateModel = new IndividualProfileLinkUpdateModel();
        updateModel.setProfileAccessLevel("READER");

        IndividualProfile existingIndividual =
                IndividualProfile.builder()
                        .id(UUID.randomUUID())
                        .ssn("ssn")
                        .primaryAddress(createAddress())
                        .mailingAddress(createAddress())
                        .build();

        when(individualProfileService.getIndividualById(profileId))
                .thenReturn(Optional.of(existingIndividual));

        UUID userId = UUID.randomUUID();
        when(individualProfileLinkService.getIndividualUserLinkByProfileAndUserId(
                        profileId, userId))
                .thenReturn(Optional.empty());

        IndividualProfileLink savedIndividualLink =
                IndividualProfileLink.builder().profile(existingIndividual).build();
        savedIndividualLink.setId(UUID.randomUUID());
        savedIndividualLink.setProfileAccessLevel(ProfileAccessLevel.ADMIN);
        PublicUser user = PublicUser.builder().build();
        user.setId(UUID.randomUUID());
        savedIndividualLink.setUser(user);
        when(userService.getUserByIdLoaded(userId)).thenReturn(Optional.of(user));

        when(individualProfileLinkService.saveIndividualUserLink(any(IndividualProfileLink.class)))
                .thenReturn(savedIndividualLink);
        when(individualProfileLinkService.getIndividualUserLinkByProfileAndUserId(
                        profileId, userId))
                .thenReturn(Optional.of(savedIndividualLink));

        when(individualProfileLinkService.saveIndividualUserLink(any(IndividualProfileLink.class)))
                .thenReturn(savedIndividualLink);
        when(individualProfileLinkService.getIndividualUserLinkByProfileAndUserId(
                        profileId, userId))
                .thenReturn(Optional.of(savedIndividualLink));

        try (MockedStatic<SecurityContextUtility> mock =
                Mockito.mockStatic(SecurityContextUtility.class)) {

            mock.when(SecurityContextUtility::getAuthenticatedUserId)
                    .thenReturn(UUID.randomUUID().toString());
            mockMvc.perform(
                            MockMvcRequestBuilders.put(
                                            "/api/v1/profiles/individuals/{profileId}/links/{userId}",
                                            profileId,
                                            userId)
                                    .contentType(
                                            org.springframework.http.MediaType.APPLICATION_JSON)
                                    .content(asJsonString(updateModel)))
                    .andExpect(MockMvcResultMatchers.status().isOk());
        }
    }

    @Test
    void testUpdateIndividualProfileLink_Forbidden() throws Exception {
        try (MockedStatic<UserUtility> mock = Mockito.mockStatic(UserUtility.class)) {
            mock.when(UserUtility::getAuthenticatedUserType).thenReturn(UserType.PUBLIC.getValue());

            UUID profileId = UUID.randomUUID();
            UUID userId = UUID.randomUUID();
            when(authorizationHandler.isAllowedForInstance(eq("link"), any())).thenReturn(false);
            when(individualProfileLinkService.getIndividualUserLinkByProfileAndUserId(
                            profileId, userId))
                    .thenReturn(Optional.empty());
            when(employerProfileLinkService.getEmployerUserLink(profileId, userId))
                    .thenReturn(Optional.empty());
            IndividualProfileLink individualUserLink = createIndividualUserLink();

            IndividualProfileLinkUpdateModel updateModel = new IndividualProfileLinkUpdateModel();
            updateModel.setProfileAccessLevel("READER");
            mockMvc.perform(
                            MockMvcRequestBuilders.put(
                                            "/api/v1/profiles/individuals/{profileId}/links/{userId}",
                                            profileId,
                                            UUID.randomUUID())
                                    .contentType(
                                            org.springframework.http.MediaType.APPLICATION_JSON)
                                    .content(asJsonString(updateModel)))
                    .andExpect(MockMvcResultMatchers.status().isForbidden());
        }
    }

    @Test
    void testUpdateIndividualProfileLink_IndividualNotFound_AfterSaving() throws Exception {
        UUID profileId = UUID.randomUUID();
        IndividualProfileLinkUpdateModel updateModel = new IndividualProfileLinkUpdateModel();
        updateModel.setProfileAccessLevel("READER");

        IndividualProfile existingIndividual =
                new IndividualProfile(); // create an instance or use Mockito to mock it

        UUID userId = UUID.randomUUID();
        when(individualProfileService.getIndividualById(profileId))
                .thenReturn(Optional.of(existingIndividual));
        when(individualProfileLinkService.getIndividualUserLinkByProfileAndUserId(
                        profileId, userId))
                .thenReturn(Optional.empty());

        IndividualProfileLink savedIndividualLink =
                new IndividualProfileLink(); // create an instance or use Mockito to mock it
        when(individualProfileLinkService.saveIndividualUserLink(any(IndividualProfileLink.class)))
                .thenReturn(savedIndividualLink);
        when(individualProfileLinkService.getIndividualUserLinkByProfileAndUserId(
                        profileId, userId))
                .thenReturn(Optional.empty());
        when(userService.getUserByIdLoaded(userId)).thenReturn(Optional.of(new PublicUser()));

        // Use assertThrows to check for 500 error, since not finding the resource after saving it
        // is in fact unexpected behavior, if that ever happens
        mockMvc.perform(
                        MockMvcRequestBuilders.put(
                                        "/api/v1/profiles/individuals/{profileId}/links/{userId}",
                                        profileId,
                                        userId)
                                .contentType(org.springframework.http.MediaType.APPLICATION_JSON)
                                .content(asJsonString(updateModel)))
                .andExpect(MockMvcResultMatchers.status().isInternalServerError());
    }

    @Test
    void testUpdateIndividualProfileLink_IndividualNotFound() throws Exception {
        UUID profileId = UUID.randomUUID();
        IndividualProfileLinkUpdateModel updateModel = new IndividualProfileLinkUpdateModel();
        updateModel.setProfileAccessLevel("READER");

        when(individualProfileService.getIndividualById(profileId)).thenReturn(Optional.empty());
        mockMvc.perform(
                        MockMvcRequestBuilders.put(
                                        "/api/v1/profiles/individuals/{profileId}/links/{userId}",
                                        profileId,
                                        UUID.randomUUID())
                                .contentType(org.springframework.http.MediaType.APPLICATION_JSON)
                                .content(asJsonString(updateModel)))
                .andExpect(MockMvcResultMatchers.status().isNotFound());
    }

    @Test
    void getIndividualProfilesLinks() throws Exception {
        IndividualProfileLink individualUserLink = createIndividualUserLink();
        Page<IndividualProfileLink> individualPage =
                new PageImpl<>(Collections.singletonList(individualUserLink));

        when(individualProfileService.getIndividualById(individualUserLink.getProfile().getId()))
                .thenReturn(Optional.of(createIndividual()));

        when(individualProfileRepository.findAll(any(Specification.class), any(Pageable.class)))
                .thenReturn(individualPage);

        when(individualProfileLinkService.getIndividualLinksByFilters(any()))
                .thenReturn(individualPage);

        mockMvc.perform(
                        get(
                                "/api/v1/profiles/individuals/{profileId}/links",
                                individualUserLink.getProfile().getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items", hasSize(1)))
                .andExpect(
                        jsonPath("$.items[0].userId")
                                .value(individualUserLink.getUser().getId().toString()))
                .andExpect(
                        jsonPath("$.items[0].profileAccessLevel")
                                .value(individualUserLink.getProfileAccessLevel().toString()))
                .andExpect(jsonPath("$.pagingMetadata.totalCount").value(1))
                .andExpect(jsonPath("$.pagingMetadata.pageNumber").value(0));
    }

    @Test
    void getIndividualProfilesLinks_Forbidden() throws Exception {
        try (MockedStatic<UserUtility> mock = Mockito.mockStatic(UserUtility.class)) {
            mock.when(UserUtility::getAuthenticatedUserType).thenReturn(UserType.PUBLIC.getValue());

            UUID profileId = UUID.randomUUID();
            UUID userId = UUID.randomUUID();
            when(authorizationHandler.isAllowedForInstance(eq("view"), any())).thenReturn(false);
            when(individualProfileLinkService.getIndividualUserLinkByProfileAndUserId(
                            profileId, userId))
                    .thenReturn(Optional.empty());
            when(employerProfileLinkService.getEmployerUserLink(profileId, userId))
                    .thenReturn(Optional.empty());
            IndividualProfileLink individualUserLink = createIndividualUserLink();
            mockMvc.perform(
                            get(
                                    "/api/v1/profiles/individuals/{profileId}/links",
                                    individualUserLink.getProfile().getId()))
                    .andExpect(status().isForbidden());
        }
    }

    @Test
    void getIndividualProfilesLinks_IndividualNotFound() throws Exception {
        IndividualProfileLink individualUserLink = createIndividualUserLink();

        when(individualProfileService.getIndividualById(individualUserLink.getProfile().getId()))
                .thenReturn(Optional.empty());

        mockMvc.perform(
                        get(
                                "/api/v1/profiles/individuals/{profileId}/links",
                                individualUserLink.getProfile().getId()))
                .andExpect(status().isNotFound());
    }

    @ParameterizedTest
    @CsvSource({"agency, false", "public, false", "public, true", "agency, true"})
    void updateEmployerProfileLink_SuccessAndForbidden(String userType, boolean isAuthorized)
            throws Exception {

        if (!isAuthorized) {
            when(authorizationHandler.isAllowedForInstance(eq("link"), any())).thenReturn(false);
        }

        UUID profileId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        EmployerProfileLinkRequestModel request = new EmployerProfileLinkRequestModel();
        request.setProfileAccessLevel("ADMIN");

        EmployerProfile employer = new EmployerProfile();
        employer.setId(profileId);
        employer.setCreatedBy(UUID.randomUUID().toString());
        EmployerProfileLink employerUserLink = new EmployerProfileLink();
        PublicUser user = new PublicUser();
        user.setId(userId);
        employerUserLink.setUser(user);
        employerUserLink.setProfile(employer);
        employerUserLink.setProfileAccessLevel(
                ProfileAccessLevel.fromValue(request.getProfileAccessLevel()));

        when(employerProfileService.getEmployerById(profileId)).thenReturn(Optional.of(employer));
        PublicUser publicUser = new PublicUser();
        publicUser.setId(userId);
        when(userService.getUserByIdLoaded(userId)).thenReturn(Optional.of(publicUser));

        EmployerProfileLink employerProfileLink =
                EmployerProfileLink.builder().profile(employer).build();
        employerProfileLink.setUser(publicUser);
        employerProfileLink.setProfileAccessLevel(
                isAuthorized ? ProfileAccessLevel.ADMIN : ProfileAccessLevel.READER);
        when(employerProfileLinkService.getEmployerUserLink(any(), any()))
                .thenReturn(Optional.of(employerProfileLink));

        when(employerProfileLinkService.saveEmployerUserLink(any(EmployerProfileLink.class)))
                .thenReturn(employerUserLink);

        try (MockedStatic<SecurityContextUtility> securityContextUtilityMock =
                        Mockito.mockStatic(SecurityContextUtility.class);
                MockedStatic<UserUtility> userUtilityMock = Mockito.mockStatic(UserUtility.class)) {

            securityContextUtilityMock
                    .when(SecurityContextUtility::getAuthenticatedUserId)
                    .thenReturn(UUID.randomUUID().toString());

            userUtilityMock.when(UserUtility::getAuthenticatedUserType).thenReturn(userType);

            var mvcRequest =
                    mockMvc.perform(
                            put(
                                            "/api/v1/profiles/employers/{profileId}/links/{userId}",
                                            profileId,
                                            userId)
                                    .contentType(MediaType.APPLICATION_JSON)
                                    .content(objectMapper.writeValueAsString(request)));

            if (isAuthorized) {
                mvcRequest
                        .andExpect(status().isOk())
                        .andExpect(jsonPath("$.userId").value(userId.toString()))
                        .andExpect(jsonPath("$.profileId").value(profileId.toString()))
                        .andExpect(
                                jsonPath("$.profileAccessLevel")
                                        .value(request.getProfileAccessLevel()));
            } else {
                mvcRequest.andExpect(status().isForbidden());
            }
        }
    }

    @Test
    void updateEmployerProfileLink_SuccessCreate() throws Exception {
        UUID profileId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        EmployerProfileLinkRequestModel request = new EmployerProfileLinkRequestModel();
        request.setProfileAccessLevel("ADMIN");

        EmployerProfile employer = new EmployerProfile();
        employer.setId(profileId);
        EmployerProfileLink employerUserLink = new EmployerProfileLink();

        PublicUser user = new PublicUser();
        user.setId(userId);
        employerUserLink.setUser(user);
        employerUserLink.setProfile(employer);
        employerUserLink.setProfileAccessLevel(
                ProfileAccessLevel.fromValue(request.getProfileAccessLevel()));

        when(employerProfileService.getEmployerById(profileId)).thenReturn(Optional.of(employer));
        when(employerProfileLinkService.getEmployerUserLink(profileId, userId))
                .thenReturn(Optional.empty())
                .thenReturn(Optional.of(employerUserLink));
        when(userService.getUserByIdLoaded(userId)).thenReturn(Optional.of(user));

        mockMvc.perform(
                        put(
                                        "/api/v1/profiles/employers/{profileId}/links/{userId}",
                                        profileId,
                                        userId)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.userId").value(userId.toString()))
                .andExpect(jsonPath("$.profileId").value(profileId.toString()))
                .andExpect(jsonPath("$.profileAccessLevel").value(request.getProfileAccessLevel()));
    }

    @Test
    void updateEmployerProfileLink_EmployerNotFound() throws Exception {
        UUID profileId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        EmployerProfileLinkRequestModel request = new EmployerProfileLinkRequestModel();
        request.setProfileAccessLevel("ADMIN");

        when(employerProfileService.getEmployerById(profileId)).thenReturn(Optional.empty());

        mockMvc.perform(
                        put(
                                        "/api/v1/profiles/employers/{profileId}/links/{userId}",
                                        profileId,
                                        userId)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isNotFound());
    }

    @ParameterizedTest
    @CsvSource({"agency, false", "public, false", "public, true", "agency, true"})
    void deleteEmployerProfileLink_SuccessAndForbidden(String userType, boolean isAuthorized)
            throws Exception {

        if (!isAuthorized) {
            when(authorizationHandler.isAllowedForInstance(eq("delete-link"), any()))
                    .thenReturn(false);
        }

        UUID profileId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        EmployerProfileLink employerUserLink = new EmployerProfileLink();
        employerUserLink.setId(UUID.randomUUID());

        EmployerProfileLink employerProfileLink =
                EmployerProfileLink.builder()
                        .profile(EmployerProfile.builder().id(profileId).build())
                        .build();
        PublicUser user = PublicUser.builder().build();
        user.setId(userId);
        employerProfileLink.setUser(user);
        employerProfileLink.setProfileAccessLevel(
                isAuthorized ? ProfileAccessLevel.ADMIN : ProfileAccessLevel.READER);

        when(employerProfileLinkService.getEmployerUserLink(any(), any()))
                .thenReturn(Optional.of(employerProfileLink));
        when(employerProfileLinkService.getEmployerUserLink(any(), any()))
                .thenReturn(Optional.of(employerProfileLink));

        ArgumentCaptor<EmployerProfileLink> employerUserLinkCaptor =
                ArgumentCaptor.forClass(EmployerProfileLink.class);

        doNothing()
                .when(employerProfileService)
                .postAuditEventForEmployerProfileUserRemoved(employerUserLinkCaptor.capture());

        String lastUpdatedBy = UUID.randomUUID().toString();

        try (MockedStatic<SecurityContextUtility> securityContextUtilityMock =
                        Mockito.mockStatic(SecurityContextUtility.class);
                MockedStatic<UserUtility> userUtilityMock = Mockito.mockStatic(UserUtility.class)) {

            securityContextUtilityMock
                    .when(SecurityContextUtility::getAuthenticatedUserId)
                    .thenReturn(lastUpdatedBy);

            userUtilityMock.when(UserUtility::getAuthenticatedUserType).thenReturn(userType);

            mockMvc.perform(
                            delete(
                                    "/api/v1/profiles/employers/{profileId}/links/{userId}",
                                    profileId,
                                    userId))
                    .andExpect(isAuthorized ? status().isNoContent() : status().isForbidden());

            if (isAuthorized) {
                assertEquals(lastUpdatedBy, employerUserLinkCaptor.getValue().getLastUpdatedBy());
            }
        }
    }

    @Test
    void deleteEmployerProfileLink_NotFound() throws Exception {
        UUID profileId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();

        when(employerProfileLinkService.getEmployerUserLink(profileId, userId))
                .thenReturn(Optional.empty());

        mockMvc.perform(
                        delete(
                                "/api/v1/profiles/employers/{profileId}/links/{userId}",
                                profileId,
                                userId))
                .andExpect(status().isNotFound());
    }

    @Test
    void getEmployerProfileLinks_Success() throws Exception {
        UUID profileId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        String name = "name";
        String email = "email";
        EmployerProfile employer = new EmployerProfile();
        employer.setId(profileId);
        EmployerProfileLink employerUserLink = new EmployerProfileLink();

        PublicUser user = PublicUser.builder().build();
        user.setId(userId);
        employerUserLink.setUser(user);
        employerUserLink.setProfile(employer);
        employerUserLink.setProfileAccessLevel(ProfileAccessLevel.fromValue("ADMIN"));

        when(employerProfileLinkService.getEmployerUserLinks(any(EmployerProfileLinkFilters.class)))
                .thenReturn(new PageImpl<>(Collections.singletonList(employerUserLink)));

        when(userService.getUsersBySearchCriteria(any(), any()))
                .thenReturn(new PageImpl<>(Collections.singletonList(user)));

        mockMvc.perform(
                        get(
                                "/api/v1/profiles/employers/"
                                        + profileId
                                        + "/links?name="
                                        + name
                                        + "&email="
                                        + email))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items[0].userId").value(userId.toString()))
                .andExpect(jsonPath("$.items[0].profileId").value(profileId.toString()))
                .andExpect(
                        jsonPath("$.items[0].profileAccessLevel")
                                .value(ProfileAccessLevel.fromValue("ADMIN").toString()));
    }

    @Test
    void testPostInvitation_Success() throws Exception {

        UUID profileId = UUID.randomUUID();
        ProfileInvitationRequestModel request =
                new ProfileInvitationRequestModel().accessLevel("ADMIN").email(USER_EMAIL);

        List<ProfileInvitation> savedProfileInvitation = new ArrayList<>();

        when(commonProfileService.getProfileById(any()))
                .thenReturn(Optional.of(new EmployerProfile()));
        when(invitationService.createProfileInvitation(any(), any(), any()))
                .thenAnswer(
                        i -> {
                            ProfileInvitation profileInvitation = i.getArgument(2);
                            profileInvitation.setId(UUID.randomUUID());
                            profileInvitation.setClaimed(false);
                            profileInvitation.setType(i.getArgument(0));
                            savedProfileInvitation.add(profileInvitation);
                            return profileInvitation;
                        });

        mockMvc.perform(
                        post("/api/v1/profiles/employers/" + profileId.toString() + "/invitations")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessLevel").value(request.getAccessLevel()))
                .andExpect(jsonPath("$.email").value(USER_EMAIL))
                .andExpect(jsonPath("$.profileId").value(profileId.toString()))
                .andExpect(jsonPath("$.profileType").value("EMPLOYER"))
                .andExpect(jsonPath("$.claimed").value(false));

        verify(invitationService)
                .postAuditEventForProfileInvite(
                        savedProfileInvitation.get(0), AuditActivityType.PROFILE_INVITATION_SENT);
    }

    @Test
    void testPostInvitation_Forbidden() throws Exception {

        UUID profileId = UUID.randomUUID();
        ProfileInvitationRequestModel request = new ProfileInvitationRequestModel();

        when(authorizationHandler.isAllowedForInstance(eq("invite"), any())).thenReturn(false);

        when(commonProfileService.getProfileById(any()))
                .thenReturn(Optional.of(new IndividualProfile()));

        mockMvc.perform(
                        post("/api/v1/profiles/employers/" + profileId.toString() + "/invitations")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.messages[0]").value("Forbidden action on the profile"));
    }

    @Test
    void testPostInvitation_FailedCreation() throws Exception {
        UUID profileId = UUID.randomUUID();
        ProfileInvitationRequestModel request = new ProfileInvitationRequestModel();

        when(commonProfileService.getProfileById(any()))
                .thenReturn(Optional.of(new IndividualProfile()));

        when(invitationService.createProfileInvitation(any(), any(), any()))
                .thenThrow(new ConflictException("Active invitation already exists"));

        mockMvc.perform(
                        post("/api/v1/profiles/individuals/"
                                        + profileId.toString()
                                        + "/invitations")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.messages[0]").value("Active invitation already exists"));
    }

    @Test
    void testGetInvitations_Success() throws Exception {
        UUID profileId = UUID.randomUUID();
        String accessLevel = "ADMIN";
        String email = USER_EMAIL;
        boolean exactEmailMatch = true;
        String sortBy = "expires";
        String sortOrder = "ASC";
        int pageNumber = 1;
        int pageSize = 10;

        ArgumentCaptor<ProfileInvitationFilters> captor =
                ArgumentCaptor.forClass(ProfileInvitationFilters.class);

        ProfileInvitation invitation =
                ProfileInvitation.builder()
                        .accessLevel(ProfileAccessLevel.ADMIN)
                        .email(USER_EMAIL)
                        .claimed(true)
                        .type(ProfileType.INDIVIDUAL)
                        .build();

        when(commonProfileService.getProfileById(any()))
                .thenReturn(Optional.of(new IndividualProfile()));
        when(invitationService.getProfileInvitationsByFilters(captor.capture()))
                .thenReturn(new PageImpl<ProfileInvitation>(Collections.singletonList(invitation)));

        mockMvc.perform(
                        get("/api/v1/profiles/individuals/" + profileId.toString() + "/invitations")
                                .param("accessLevel", accessLevel)
                                .param("email", email)
                                .param("exactEmailMatch", String.valueOf(exactEmailMatch))
                                .param("sortBy", sortBy)
                                .param("sortOrder", sortOrder)
                                .param("pageNumber", String.valueOf(pageNumber))
                                .param("pageSize", String.valueOf(pageSize))
                                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(
                        jsonPath("$.items[0].accessLevel")
                                .value(invitation.getAccessLevel().getValue()))
                .andExpect(jsonPath("$.items[0].email").value(invitation.getEmail()))
                .andExpect(jsonPath("$.items[0].claimed").value(invitation.getClaimed()));

        // verify all filters are properly passed
        ProfileInvitationFilters filters = captor.getValue();
        assertEquals(accessLevel, filters.getAccessLevel());
        assertEquals(email, filters.getEmail());
        assertEquals(exactEmailMatch, filters.getExactEmailMatch());
        assertEquals(sortBy, filters.getSortBy());
        assertEquals(sortOrder, filters.getSortOrder());
        assertEquals(pageNumber, filters.getPageNumber());
        assertEquals(pageSize, filters.getPageSize());
        assertEquals(profileId, filters.getProfileId());
        assertEquals(ProfileType.INDIVIDUAL.getValue(), filters.getType());
    }

    @Test
    void testGetInvitations_Forbidden() throws Exception {
        when(commonProfileService.getProfileById(any()))
                .thenReturn(Optional.of(new EmployerProfile()));

        when(authorizationHandler.isAllowedForInstance(eq("view"), any())).thenReturn(false);

        mockMvc.perform(
                        get("/api/v1/profiles/individuals/"
                                        + UUID.randomUUID().toString()
                                        + "/invitations")
                                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.messages[0]").value("Profile not found"));
    }

    @Test
    void testGetInvitationById_Owner() throws Exception {
        UUID invitationId = UUID.randomUUID();

        String email = USER_EMAIL;
        ProfileInvitation invitation =
                ProfileInvitation.builder()
                        .accessLevel(ProfileAccessLevel.ADMIN)
                        .email(email)
                        .claimed(true)
                        .type(ProfileType.INDIVIDUAL)
                        .build();

        when(commonProfileService.getProfileById(any()))
                .thenReturn(Optional.of(new IndividualProfile()));
        when(invitationService.getProfileInvitationById(invitationId))
                .thenReturn(Optional.of(invitation));

        ResultActions response;
        try (MockedStatic<SecurityContextUtility> mocked =
                Mockito.mockStatic(SecurityContextUtility.class)) {

            mocked.when(SecurityContextUtility::getAuthenticatedUserEmail).thenReturn(email);

            response =
                    mockMvc.perform(
                            get("/api/v1/profiles/individuals/invitations/" + invitationId));
        }

        response.andExpect(status().isOk())
                .andExpect(jsonPath("$.accessLevel").value(invitation.getAccessLevel().getValue()))
                .andExpect(jsonPath("$.email").value(invitation.getEmail()))
                .andExpect(jsonPath("$.claimed").value(invitation.getClaimed()));
    }

    @Test
    void testGetInvitationById_NonOwnerButProfileAccess() throws Exception {
        UUID invitationId = UUID.randomUUID();

        String email = USER_EMAIL;
        ProfileInvitation invitation =
                ProfileInvitation.builder()
                        .accessLevel(ProfileAccessLevel.ADMIN)
                        .email(email)
                        .claimed(true)
                        .type(ProfileType.INDIVIDUAL)
                        .build();

        when(commonProfileService.getProfileById(any()))
                .thenReturn(Optional.of(new IndividualProfile()));
        when(invitationService.getProfileInvitationById(invitationId))
                .thenReturn(Optional.of(invitation));

        ResultActions response;
        try (MockedStatic<SecurityContextUtility> mocked =
                Mockito.mockStatic(SecurityContextUtility.class)) {

            mocked.when(SecurityContextUtility::getAuthenticatedUserEmail)
                    .thenReturn("another@b.c");

            response =
                    mockMvc.perform(
                            get("/api/v1/profiles/individuals/invitations/" + invitationId));
        }

        response.andExpect(status().isOk())
                .andExpect(jsonPath("$.accessLevel").value(invitation.getAccessLevel().getValue()))
                .andExpect(jsonPath("$.email").value(invitation.getEmail()))
                .andExpect(jsonPath("$.claimed").value(invitation.getClaimed()));
    }

    @Test
    void testGetInvitationById_NonOwnerAndUnauthorized() throws Exception {
        UUID invitationId = UUID.randomUUID();

        String email = USER_EMAIL;
        ProfileInvitation invitation = ProfileInvitation.builder().email(email).build();

        when(commonProfileService.getProfileById(any()))
                .thenReturn(Optional.of(new IndividualProfile()));
        when(invitationService.getProfileInvitationById(invitationId))
                .thenReturn(Optional.of(invitation));
        when(authorizationHandler.isAllowedForInstance(eq("view"), any())).thenReturn(false);

        ResultActions response;
        try (MockedStatic<SecurityContextUtility> mocked =
                Mockito.mockStatic(SecurityContextUtility.class)) {

            mocked.when(SecurityContextUtility::getAuthenticatedUserEmail)
                    .thenReturn("another@b.c");

            response =
                    mockMvc.perform(
                            get("/api/v1/profiles/individuals/invitations/" + invitationId));
        }

        response.andExpect(status().isNotFound())
                .andExpect(jsonPath("$.messages[0]").value("Profile invitation not found"));
    }

    @Test
    void testClaimInvitation_Success() throws Exception {

        UUID invitationId = UUID.randomUUID();
        String email = USER_EMAIL;
        ProfileInvitation invitation =
                ProfileInvitation.builder()
                        .accessLevel(ProfileAccessLevel.ADMIN)
                        .email(email)
                        .claimed(false)
                        .profileId(UUID.randomUUID())
                        .type(ProfileType.INDIVIDUAL)
                        .build();

        when(invitationService.getProfileInvitationById(invitationId))
                .thenReturn(Optional.of(invitation));
        when(commonProfileService.getProfileById(any()))
                .thenReturn(Optional.of(new IndividualProfile()));

        when(individualProfileService.getIndividualById(invitation.getProfileId()))
                .thenReturn(Optional.of(new IndividualProfile()));

        when(userService.getUserByIdLoaded(any())).thenReturn(Optional.of(new PublicUser()));

        when(individualProfileLinkService.getIndividualUserLinkByProfileAndUserId(any(), any()))
                .thenReturn(Optional.empty())
                .thenReturn(Optional.of(new IndividualProfileLink()));

        ResultActions response;
        try (MockedStatic<SecurityContextUtility> mocked =
                        Mockito.mockStatic(SecurityContextUtility.class);
                MockedStatic<UserUtility> userUtilityMock = Mockito.mockStatic(UserUtility.class)) {

            userUtilityMock
                    .when(UserUtility::getAuthenticatedUserType)
                    .thenReturn(UserType.PUBLIC.getValue());

            mocked.when(SecurityContextUtility::getAuthenticatedUserEmail).thenReturn(email);
            mocked.when(SecurityContextUtility::getAuthenticatedUserId)
                    .thenReturn(UUID.randomUUID().toString());

            response =
                    mockMvc.perform(
                            post(
                                    "/api/v1/profiles/individuals/invitations/"
                                            + invitationId
                                            + "/claim"));
        }

        response.andExpect(status().isNoContent());

        verify(invitationService).verifyInvitationIsClaimable(invitation);
        verify(invitationService).markAsClaimedAndSave(invitation);
        verify(invitationService)
                .postAuditEventForProfileInvite(
                        invitation, AuditActivityType.PROFILE_INVITATION_CLAIMED);
    }

    @Test
    void testClaimInvitation_NotOwner() throws Exception {

        UUID invitationId = UUID.randomUUID();
        String email = USER_EMAIL;
        ProfileInvitation invitation = ProfileInvitation.builder().email(email).build();

        when(invitationService.getProfileInvitationById(invitationId))
                .thenReturn(Optional.of(invitation));

        ResultActions response;
        try (MockedStatic<SecurityContextUtility> mocked =
                        Mockito.mockStatic(SecurityContextUtility.class);
                MockedStatic<UserUtility> userUtilityMock = Mockito.mockStatic(UserUtility.class)) {

            userUtilityMock
                    .when(UserUtility::getAuthenticatedUserType)
                    .thenReturn(UserType.PUBLIC.getValue());

            mocked.when(SecurityContextUtility::getAuthenticatedUserEmail)
                    .thenReturn("another@b.c");

            response =
                    mockMvc.perform(
                            post(
                                    "/api/v1/profiles/individuals/invitations/"
                                            + invitationId
                                            + "/claim"));
        }
        response.andExpect(status().isNotFound())
                .andExpect(jsonPath("$.messages[0]").value("Profile invitation not found"));
    }

    @Test
    void testClaimInvitation_WrongProfileType() throws Exception {

        UUID invitationId = UUID.randomUUID();
        String email = USER_EMAIL;
        ProfileInvitation invitation =
                ProfileInvitation.builder().email(email).type(ProfileType.EMPLOYER).build();

        when(invitationService.getProfileInvitationById(invitationId))
                .thenReturn(Optional.of(invitation));

        ResultActions response;
        try (MockedStatic<SecurityContextUtility> mocked =
                        Mockito.mockStatic(SecurityContextUtility.class);
                MockedStatic<UserUtility> userUtilityMock = Mockito.mockStatic(UserUtility.class)) {

            userUtilityMock
                    .when(UserUtility::getAuthenticatedUserType)
                    .thenReturn(UserType.PUBLIC.getValue());

            mocked.when(SecurityContextUtility::getAuthenticatedUserEmail).thenReturn(email);

            response =
                    mockMvc.perform(
                            post(
                                    "/api/v1/profiles/individuals/invitations/"
                                            + invitationId
                                            + "/claim"));
        }

        response.andExpect(status().isBadRequest())
                .andExpect(
                        jsonPath("$.messages[0]")
                                .value(
                                        "Profile invitation is not for the required profile type"
                                                + " (INDIVIDUAL)."));
    }

    @Test
    void testClaimInvitation_NotClaimable() throws Exception {
        UUID invitationId = UUID.randomUUID();
        String email = USER_EMAIL;
        ProfileInvitation invitation =
                ProfileInvitation.builder().email(email).type(ProfileType.EMPLOYER).build();

        when(invitationService.getProfileInvitationById(invitationId))
                .thenReturn(Optional.of(invitation));

        doThrow(new ConflictException("Profile invitation already claimed"))
                .when(invitationService)
                .verifyInvitationIsClaimable(invitation);

        ResultActions response;
        try (MockedStatic<SecurityContextUtility> mocked =
                        Mockito.mockStatic(SecurityContextUtility.class);
                MockedStatic<UserUtility> userUtilityMock = Mockito.mockStatic(UserUtility.class)) {

            userUtilityMock
                    .when(UserUtility::getAuthenticatedUserType)
                    .thenReturn(UserType.PUBLIC.getValue());
            mocked.when(SecurityContextUtility::getAuthenticatedUserEmail).thenReturn(email);

            response =
                    mockMvc.perform(
                            post(
                                    "/api/v1/profiles/employers/invitations/"
                                            + invitationId
                                            + "/claim"));
        }

        response.andExpect(status().isConflict())
                .andExpect(jsonPath("$.messages[0]").value("Profile invitation already claimed"));
    }

    @Test
    void testClaimInvitation_ProfileNotFound() throws Exception {

        UUID invitationId = UUID.randomUUID();
        String email = USER_EMAIL;
        ProfileInvitation invitation =
                ProfileInvitation.builder()
                        .email(email)
                        .profileId(UUID.randomUUID())
                        .type(ProfileType.EMPLOYER)
                        .build();

        when(invitationService.getProfileInvitationById(invitationId))
                .thenReturn(Optional.of(invitation));
        when(employerProfileService.getEmployerById(invitation.getProfileId()))
                .thenReturn(Optional.empty());

        ResultActions response;
        try (MockedStatic<SecurityContextUtility> mocked =
                        Mockito.mockStatic(SecurityContextUtility.class);
                MockedStatic<UserUtility> userUtilityMock = Mockito.mockStatic(UserUtility.class)) {

            userUtilityMock
                    .when(UserUtility::getAuthenticatedUserType)
                    .thenReturn(UserType.PUBLIC.getValue());
            mocked.when(SecurityContextUtility::getAuthenticatedUserEmail).thenReturn(email);

            response =
                    mockMvc.perform(
                            post(
                                    "/api/v1/profiles/employers/invitations/"
                                            + invitationId
                                            + "/claim"));
        }
        response.andExpect(status().isNotFound())
                .andExpect(jsonPath("$.messages[0]").value("Employer profile not found"));
    }

    @Test
    void testClaimInvitation_WrongUserType() throws Exception {

        ResultActions response;
        try (MockedStatic<UserUtility> userUtilityMock = Mockito.mockStatic(UserUtility.class)) {
            userUtilityMock
                    .when(UserUtility::getAuthenticatedUserType)
                    .thenReturn(UserType.AGENCY.getValue());

            response =
                    mockMvc.perform(
                            post(
                                    "/api/v1/profiles/employers/invitations/"
                                            + UUID.randomUUID()
                                            + "/claim"));
        }
        response.andExpect(status().isForbidden())
                .andExpect(
                        jsonPath("$.messages[0]")
                                .value("Claiming invitations is intended for public users only."));
    }

    @Test
    void testDeleteInvitation_Success() throws Exception {

        UUID invitationId = UUID.randomUUID();
        UUID profileId = UUID.randomUUID();
        ProfileInvitation invitation =
                ProfileInvitation.builder()
                        .profileId(profileId)
                        .type(ProfileType.INDIVIDUAL)
                        .build();

        when(commonProfileService.getProfileById(any()))
                .thenReturn(Optional.of(new IndividualProfile()));

        when(invitationService.getProfileInvitationById(invitationId))
                .thenReturn(Optional.of(invitation));

        mockMvc.perform(
                        delete(
                                "/api/v1/profiles/individuals/"
                                        + profileId.toString()
                                        + "/invitations/"
                                        + invitationId))
                .andExpect(status().isNoContent());

        verify(invitationService).deleteProfileInvitation(invitationId);
        verify(invitationService)
                .postAuditEventForProfileInvite(
                        invitation, AuditActivityType.PROFILE_INVITATION_DELETED);

        verify(invitationService).deleteProfileInvitation(invitationId);
        verify(invitationService)
                .postAuditEventForProfileInvite(
                        invitation, AuditActivityType.PROFILE_INVITATION_DELETED);
    }

    @Test
    void testDeleteInvitation_Forbidden() throws Exception {

        UUID profileId = UUID.randomUUID();
        UUID invitationId = UUID.randomUUID();

        when(commonProfileService.getProfileById(any()))
                .thenReturn(Optional.of(new IndividualProfile()));

        when(authorizationHandler.isAllowedForInstance(eq("invite"), any())).thenReturn(false);

        mockMvc.perform(
                        delete(
                                "/api/v1/profiles/individuals/"
                                        + profileId.toString()
                                        + "/invitations/"
                                        + invitationId))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.messages[0]").value("Forbidden action on the profile"));
    }

    @Test
    void testDeleteInvitation_NotFound() throws Exception {

        UUID invitationId = UUID.randomUUID();
        UUID profileId = UUID.randomUUID();

        when(commonProfileService.getProfileById(any()))
                .thenReturn(Optional.of(new IndividualProfile()));

        when(invitationService.getProfileInvitationById(invitationId)).thenReturn(Optional.empty());

        mockMvc.perform(
                        delete(
                                "/api/v1/profiles/individuals/"
                                        + profileId.toString()
                                        + "/invitations/"
                                        + invitationId))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.messages[0]").value("Profile invitation not found"));
    }

    @Test
    void testDeleteInvitation_WrongType() throws Exception {

        UUID invitationId = UUID.randomUUID();
        UUID profileId = UUID.randomUUID();
        ProfileInvitation invitation =
                ProfileInvitation.builder().profileId(profileId).type(ProfileType.EMPLOYER).build();

        when(commonProfileService.getProfileById(any()))
                .thenReturn(Optional.of(new IndividualProfile()));

        when(invitationService.getProfileInvitationById(invitationId))
                .thenReturn(Optional.of(invitation));

        mockMvc.perform(
                        delete(
                                "/api/v1/profiles/individuals/"
                                        + profileId.toString()
                                        + "/invitations/"
                                        + invitationId))
                .andExpect(status().isBadRequest())
                .andExpect(
                        jsonPath("$.messages[0]")
                                .value(
                                        "Profile invitation is not for the required profile type"
                                                + " (INDIVIDUAL)."));
    }

    @Test
    void testDeleteInvitation_AlreadyClaimed() throws Exception {

        UUID invitationId = UUID.randomUUID();
        UUID profileId = UUID.randomUUID();
        ProfileInvitation invitation =
                ProfileInvitation.builder()
                        .profileId(profileId)
                        .type(ProfileType.EMPLOYER)
                        .claimed(true)
                        .build();

        when(commonProfileService.getProfileById(any()))
                .thenReturn(Optional.of(new EmployerProfile()));

        when(invitationService.getProfileInvitationById(invitationId))
                .thenReturn(Optional.of(invitation));

        mockMvc.perform(
                        delete(
                                "/api/v1/profiles/employers/"
                                        + profileId.toString()
                                        + "/invitations/"
                                        + invitationId))
                .andExpect(status().isConflict())
                .andExpect(
                        jsonPath("$.messages[0]")
                                .value(
                                        "Cannot delete an invitation that has already been"
                                                + " claimed."));
    }

    @Test
    void testResendInvitation_Success() throws Exception {

        UUID invitationId = UUID.randomUUID();
        UUID profileId = UUID.randomUUID();
        ProfileInvitation invitation =
                ProfileInvitation.builder().profileId(profileId).type(ProfileType.EMPLOYER).build();

        when(commonProfileService.getProfileById(any()))
                .thenReturn(Optional.of(EmployerProfile.builder().legalName("The Name").build()));

        when(invitationService.getProfileInvitationById(invitationId))
                .thenReturn(Optional.of(invitation));

        mockMvc.perform(
                        post(
                                "/api/v1/profiles/employers/"
                                        + profileId.toString()
                                        + "/invitations/"
                                        + invitationId
                                        + "/resend"))
                .andExpect(status().isNoContent());

        verify(invitationService).resendExistingInvitation(invitation, "The Name");
        verify(invitationService)
                .postAuditEventForProfileInvite(
                        invitation, AuditActivityType.PROFILE_INVITATION_RESENT);
    }

    @Test
    void testResendInvitation_Forbidden() throws Exception {

        UUID invitationId = UUID.randomUUID();
        UUID profileId = UUID.randomUUID();

        when(commonProfileService.getProfileById(any()))
                .thenReturn(Optional.of(EmployerProfile.builder().build()));

        when(authorizationHandler.isAllowedForInstance(eq("invite"), any())).thenReturn(false);

        mockMvc.perform(
                        post(
                                "/api/v1/profiles/employers/"
                                        + profileId.toString()
                                        + "/invitations/"
                                        + invitationId
                                        + "/resend"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.messages[0]").value("Forbidden action on the profile"));

        verify(invitationService, never()).resendExistingInvitation(any(), anyString());
        verify(invitationService, never()).postAuditEventForProfileInvite(any(), any());
    }

    @Test
    void testResendInvitation_NotFound() throws Exception {

        UUID invitationId = UUID.randomUUID();
        UUID profileId = UUID.randomUUID();

        when(commonProfileService.getProfileById(any()))
                .thenReturn(Optional.of(IndividualProfile.builder().build()));

        when(invitationService.getProfileInvitationById(invitationId)).thenReturn(Optional.empty());

        mockMvc.perform(
                        post(
                                "/api/v1/profiles/individuals/"
                                        + profileId.toString()
                                        + "/invitations/"
                                        + invitationId
                                        + "/resend"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.messages[0]").value("Profile invitation not found"));
    }

    private IndividualProfileLink createIndividualUserLink() {
        IndividualProfileLink individualProfileLink =
                IndividualProfileLink.builder().profile(createIndividual()).build();
        PublicUser user = PublicUser.builder().build();
        user.setId(UUID.randomUUID());
        individualProfileLink.setUser(user);
        individualProfileLink.setProfileAccessLevel(ProfileAccessLevel.ADMIN);

        return individualProfileLink;
    }

    private String asJsonString(Object obj) throws Exception {
        ObjectMapper objectMapper = new ObjectMapper();
        return objectMapper.writeValueAsString(obj);
    }

    private Address createAddress() {
        return Address.builder()
                .address1("address1")
                .address2("address2")
                .city("city")
                .state("state")
                .postalCode("postalCode")
                .country("country")
                .county("county")
                .build();
    }

    private EmployerProfile createEmployer() {
        return EmployerProfile.builder()
                .id(UUID.randomUUID())
                .fein("fein")
                .legalName("legalName")
                .otherNames(Collections.singletonList("otherNames"))
                .type("LLC")
                .industry("industry")
                .summaryOfBusiness("summaryOfBusiness")
                .businessPhone("businessPhone")
                .mailingAddress(createAddress())
                .locations(List.of(createAddress()))
                .build();
    }

    private AddressModel createAddressModel() {
        AddressModel addressModel = new AddressModel();
        addressModel.address1("address1");
        addressModel.address2("address2");
        addressModel.city("city");
        addressModel.state("state");
        addressModel.postalCode("postalCode");
        addressModel.country("country");
        addressModel.county("county");

        return addressModel;
    }

    private EmployerProfileCreateModel employerProfileCreateModel() {
        EmployerProfileCreateModel employerProfileCreateModel = new EmployerProfileCreateModel();
        employerProfileCreateModel.setFein("fein");
        employerProfileCreateModel.setLegalName("legalName");
        employerProfileCreateModel.setOtherNames(Collections.singletonList("otherNames"));
        employerProfileCreateModel.setType("LLC");
        employerProfileCreateModel.setIndustry("industry");
        employerProfileCreateModel.setSummaryOfBusiness("summaryOfBusiness");
        employerProfileCreateModel.setMailingAddress(createAddressModel());
        employerProfileCreateModel.setLocations(List.of(createAddressModel()));
        employerProfileCreateModel.businessPhone("businessPhone");

        return employerProfileCreateModel;
    }

    private EmployerProfileUpdateModel employerProfileUpdateModel() {
        EmployerProfileUpdateModel employerProfileUpdateModel = new EmployerProfileUpdateModel();
        employerProfileUpdateModel.setFein("fein - changed");
        employerProfileUpdateModel.setLegalName("legalName - changed");
        employerProfileUpdateModel.setOtherNames(Collections.singletonList("otherNames - changed"));
        employerProfileUpdateModel.setType("LLC");
        employerProfileUpdateModel.setIndustry("industry - changed");
        employerProfileUpdateModel.setSummaryOfBusiness("summaryOfBusiness - changed");
        employerProfileUpdateModel.setMailingAddress(createAddressModel());
        employerProfileUpdateModel.setLocations(List.of(createAddressModel()));
        employerProfileUpdateModel.businessPhone("businessPhone - changed");

        return employerProfileUpdateModel;
    }

    private IndividualProfile createIndividual() {
        return IndividualProfile.builder()
                .id(UUID.randomUUID())
                .ssn("ssn")
                .primaryAddress(createAddress())
                .mailingAddress(createAddress())
                .build();
    }

    private IndividualProfileCreateModel individualProfileCreateModel() {
        IndividualProfileCreateModel individualProfileCreateModel =
                new IndividualProfileCreateModel();
        individualProfileCreateModel.setSsn("ssn");
        individualProfileCreateModel.setEmail("email@email.com");
        individualProfileCreateModel.setFirstName("First");
        individualProfileCreateModel.setLastName("Last");
        individualProfileCreateModel.setPhoneNumber("3331112222");
        individualProfileCreateModel.setPrimaryAddress(createAddressModel());
        individualProfileCreateModel.setMailingAddress(createAddressModel());
        return individualProfileCreateModel;
    }

    private IndividualProfileUpdateModel individualProfileUpdateModel() {
        IndividualProfileUpdateModel individualProfileUpdateModel =
                new IndividualProfileUpdateModel();
        individualProfileUpdateModel.setSsn("ssn");
        individualProfileUpdateModel.setEmail("email@email.com");
        individualProfileUpdateModel.setFirstName("First");
        individualProfileUpdateModel.setLastName("Last");
        individualProfileUpdateModel.setPhoneNumber("3331112222");
        individualProfileUpdateModel.setPrimaryAddress(createAddressModel());
        individualProfileUpdateModel.setMailingAddress(createAddressModel());
        return individualProfileUpdateModel;
    }
}
