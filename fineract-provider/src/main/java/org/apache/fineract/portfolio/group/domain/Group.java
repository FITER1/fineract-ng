/**
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements. See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership. The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.fineract.portfolio.group.domain;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;
import org.apache.commons.lang3.ObjectUtils;
import org.apache.fineract.infrastructure.codes.domain.CodeValue;
import org.apache.fineract.infrastructure.core.data.ApiParameterError;
import org.apache.fineract.infrastructure.core.domain.AbstractPersistableCustom;
import org.apache.fineract.infrastructure.core.exception.GeneralPlatformDomainRuleException;
import org.apache.fineract.infrastructure.core.exception.PlatformApiDataValidationException;
import org.apache.fineract.infrastructure.core.service.DateUtils;
import org.apache.fineract.organisation.office.domain.Office;
import org.apache.fineract.organisation.staff.domain.Staff;
import org.apache.fineract.portfolio.client.domain.Client;
import org.apache.fineract.portfolio.group.api.GroupingTypesApiConstants;
import org.apache.fineract.portfolio.group.exception.*;
import org.apache.fineract.useradministration.domain.AppUser;
import org.hibernate.annotations.LazyCollection;
import org.hibernate.annotations.LazyCollectionOption;
import org.joda.time.LocalDate;

import javax.persistence.*;
import java.util.*;
import java.util.stream.Collectors;

@SuperBuilder(toBuilder = true)
@Data
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
@Entity
@Table(name = "m_group")
public final class Group extends AbstractPersistableCustom<Long> {

    @Column(name = "external_id", length = 100, unique = true)
    private String externalId;

    /**
     * A value from {@link GroupingTypeStatus}.
     */
    @Column(name = "status_enum", nullable = false)
    private Integer status;

    @Column(name = "activation_date")
    // @Temporal(TemporalType.DATE)
    private Date activationDate;

    @ManyToOne(optional = true)
    @JoinColumn(name = "activatedon_userid")
    private AppUser activatedBy;

    @ManyToOne
    @JoinColumn(name = "office_id", nullable = false)
    private Office office;

    @ManyToOne
    @JoinColumn(name = "staff_id")
    private Staff staff;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "parent_id")
    private Group parent;

    @ManyToOne
    @JoinColumn(name = "level_id", nullable = false)
    private GroupLevel groupLevel;

    @Column(name = "display_name", length = 100, unique = true)
    private String name;

    @Column(name = "hierarchy", length = 100)
    private String hierarchy;

    @OneToMany
    @LazyCollection(LazyCollectionOption.FALSE)
    @JoinColumn(name = "parent_id")
    private List<Group> groupMembers = new LinkedList<>();

    @ManyToMany
    @JoinTable(name = "m_group_client", joinColumns = @JoinColumn(name = "group_id"), inverseJoinColumns = @JoinColumn(name = "client_id"))
    private Set<Client> clientMembers = new HashSet<>();

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "closure_reason_cv_id")
    private CodeValue closureReason;

    @Column(name = "closedon_date")
    // @Temporal(TemporalType.DATE)
    private Date closureDate;

    @ManyToOne(optional = true, fetch=FetchType.LAZY)
    @JoinColumn(name = "closedon_userid")
    private AppUser closedBy;

    @Column(name = "submittedon_date")
    // @Temporal(TemporalType.DATE)
    private Date submittedOnDate;

    @ManyToOne(optional = true, fetch=FetchType.LAZY)
    @JoinColumn(name = "submittedon_userid")
    private AppUser submittedBy;

    @OneToMany(cascade = CascadeType.ALL, mappedBy = "center", orphanRemoval = true)
    private Set<StaffAssignmentHistory> staffHistory;
    
    @Column(name = "account_no", length = 20, unique = true, nullable = false)
    private String accountNumber;
    
    @Transient
    private boolean accountNumberRequiresAutoGeneration;

    @OneToMany(mappedBy="group",cascade = CascadeType.REMOVE)
    private Set<GroupRole> groupRole;

    // TODO: @Aleks there is way too much code in this class; will probably get better when we introduce Mapstruct

    public void activate(final AppUser currentUser, final LocalDate activationLocalDate) {

        final List<ApiParameterError> dataValidationErrors = new ArrayList<>();
        activate(currentUser, activationLocalDate, dataValidationErrors);
        if (this.groupLevel.isCenter() && this.staff != null) {
            Staff staff = this.getStaff();
            this.reassignStaff(staff, activationLocalDate);
        }
        throwExceptionIfErrors(dataValidationErrors);

    }

    public void activate(final AppUser currentUser, final LocalDate activationLocalDate, final List<ApiParameterError> dataValidationErrors) {
        validateStatusNotEqualToActiveAndLogError(dataValidationErrors);
        if (dataValidationErrors.isEmpty()) {
            this.status = GroupingTypeStatus.ACTIVE.getValue();
            setActivationDate(activationLocalDate.toDate(), currentUser, dataValidationErrors);
        }

    }

    public void throwExceptionIfErrors(final List<ApiParameterError> dataValidationErrors) {
        if (!dataValidationErrors.isEmpty()) {
            throw new PlatformApiDataValidationException(dataValidationErrors);
        }
    }

    public boolean isActivatedAfter(final LocalDate submittedOn) {
        return getActivationLocalDate().isAfter(submittedOn);
    }

    public boolean isActive() {
        return this.status != null && GroupingTypeStatus.fromInt(this.status).isActive();
    }

    public LocalDate getSubmittedOnDate() {
        return ObjectUtils.defaultIfNull(new LocalDate(this.submittedOnDate), null);
    }

    public LocalDate getActivationLocalDate() {
        LocalDate activationLocalDate = null;
        if (this.activationDate != null) {
            activationLocalDate = new LocalDate(this.activationDate);
        }
        return activationLocalDate;
    }

    public List<String> associateClients(final Set<Client> clientMembersSet) {
        final List<String> differences = new ArrayList<>();
        for (final Client client : clientMembersSet) {
            if (hasClientAsMember(client)) { throw new ClientExistInGroupException(client.getId(), getId()); }
            this.clientMembers.add(client);
            differences.add(client.getId().toString());
        }

        return differences;
    }

    public List<String> disassociateClients(final Set<Client> clientMembersSet) {
        final List<String> differences = new ArrayList<>();
        for (final Client client : clientMembersSet) {
            if (hasClientAsMember(client)) {
                this.clientMembers.remove(client);
                differences.add(client.getId().toString());
            } else {
                throw new ClientNotInGroupException(client.getId(), getId());
            }
        }

        return differences;
    }

    public boolean hasClientAsMember(final Client client) {
        return this.clientMembers.contains(client);
    }

	public void generateHierarchy() {
		if (this.parent != null) {
			this.hierarchy = this.parent.hierarchyOf(getId());
		} else {
			this.hierarchy = "." + getId() + ".";
			for (Group group : this.groupMembers) {
				group.setParent(this);
				group.generateHierarchy();
			}
		}
	}
    
	public void resetHierarchy() {
			this.hierarchy = "." + this.getId();
	}
    
    public boolean isOfficeIdentifiedBy(final Long officeId) {
        if(this.office==null || this.office.getId()==null) {
            return false;
        }
        return this.office.getId().equals(officeId);
    }

    public Long getOfficeId() {
        return this.office!=null ? this.office.getId() : null;
    }

    public void updateStaff(final Staff staff) {
        if (this.groupLevel.isCenter() && this.isActive()) {
            LocalDate updatedDate = DateUtils.getLocalDateOfTenant();
            reassignStaff(staff, updatedDate);
        }
        this.staff = staff;
    }

    public void unassignStaff() {
        if (this.groupLevel.isCenter() && this.isActive()) {
            LocalDate dateOfStaffUnassigned = DateUtils.getLocalDateOfTenant();
            removeStaff(dateOfStaffUnassigned);
        }
        this.staff = null;
    }

    public boolean isTransferInProgress() {
        return GroupingTypeStatus.fromInt(this.status).isTransferInProgress();
    }

    public boolean isTransferOnHold() {
        return GroupingTypeStatus.fromInt(this.status).isTransferOnHold();
    }

    public boolean isTransferInProgressOrOnHold() {
        return isTransferInProgress() || isTransferOnHold();
    }

    public boolean isChildClient(final Long clientId) {
        if (clientId != null && this.clientMembers != null) {
            return this.clientMembers.stream()
                .anyMatch(c -> Objects.equals(c.getId(), clientId));
        }
        return false;
    }

    public void close(final AppUser currentUser, final CodeValue closureReason, final LocalDate closureDate) {

        if (GroupingTypeStatus.fromInt(this.status).isClosed()) {
            final String errorMessage = "Group with identifier " + getId() + " is alread closed.";
            throw new InvalidGroupStateTransitionException(this.groupLevel.getLevelName(), "close", "already.closed", errorMessage, getId());
        }

        if (!GroupingTypeStatus.fromInt(this.status).isPending() && getActivationLocalDate().isAfter(closureDate)) {
            final String errorMessage = "The Group closure Date " + closureDate + " cannot be before the group Activation Date "
                    + getActivationLocalDate() + ".";
            throw new InvalidGroupStateTransitionException(this.groupLevel.getLevelName(), "close",
                    "date.cannot.before.group.actvation.date", errorMessage, closureDate, getActivationLocalDate());
        }

        this.closureReason = closureReason;
        this.closureDate = closureDate.toDate();
        this.status = GroupingTypeStatus.CLOSED.getValue();
        this.closedBy = currentUser;
    }

    public boolean hasActiveClients() {
        return this.clientMembers.stream()
            .filter(Client::isClosed)
            .map(Client::isClosed)
            .findFirst()
            .orElse(false);
    }

    public boolean hasGroupAsMember(final Group group) {
        return this.groupMembers.contains(group);
    }

    public List<String> associateGroups(final Set<Group> groupMembersSet) {

        final List<String> differences = new ArrayList<>();
        for (final Group group : groupMembersSet) {

            if (group.getGroupLevel().isCenter()) {
                final String defaultUserMessage = "Center can not assigned as a child";
                throw new GeneralPlatformDomainRuleException("error.msg.center.cannot.be.assigned.as.child", defaultUserMessage,
                        group.getId());
            }

            if (hasGroupAsMember(group)) { throw new GroupExistsInCenterException(getId(), group.getId()); }

            if (group.getParent()!=null) {
                final String defaultUserMessage = "Group is already associated with a center";
                throw new GeneralPlatformDomainRuleException("error.msg.group.already.associated.with.center", defaultUserMessage, group
                        .getParent().getId(), group.getId());
            }

            this.groupMembers.add(group);
            differences.add(group.getId().toString());
            group.setParent(this);
    		group.generateHierarchy();
        }

        return differences;
    }

    public List<String> disassociateGroups(Set<Group> groupMembersSet) {

        final List<String> differences = new ArrayList<>();
        for (final Group group : groupMembersSet) {
            if (hasGroupAsMember(group)) {
                this.groupMembers.remove(group);
                differences.add(group.getId().toString());
    			group.resetHierarchy();
            } else {
                throw new GroupNotExistsInCenterException(group.getId(), getId());
            }
        }

        return differences;
    }

    public Boolean isGroupsClientCountWithinMinMaxRange(Integer minClients, Integer maxClients) {

        if (maxClients == null && minClients == null) { return true; }

        // set minClients or maxClients to 0 if null

        if (minClients == null) {
            minClients = 0;
        }

        if (maxClients == null) {
            maxClients = Integer.MAX_VALUE;
        }

        Set<Client> activeClientMembers = getActiveClientMembers();

        if (activeClientMembers.size() >= minClients && activeClientMembers.size() <= maxClients) { return true; }
        return false;
    }

    public Boolean isGroupsClientCountWithinMaxRange(Integer maxClients) {
        Set<Client> activeClientMembers = getActiveClientMembers();
        if (maxClients == null) {
            return true;
        } else if (activeClientMembers.size() <= maxClients) {
            return true;
        } else {
            return false;
        }
    }

    public Set<Client> getActiveClientMembers() {
        return this.clientMembers.stream()
            .filter(Client::isActive)
            .collect(Collectors.toSet());
    }

    // StaffAssignmentHistory[during center creation]
    public void captureStaffHistoryDuringCenterCreation(final Staff newStaff, final LocalDate assignmentDate) {
        if (this.groupLevel.isCenter() && this.isActive() && staff != null) {
            this.staff = newStaff;
            final StaffAssignmentHistory staffAssignmentHistory = StaffAssignmentHistory.builder()
                .center(this)
                .staff(this.staff)
                .startDate(assignmentDate.toDate())
                .build();
            if (staffAssignmentHistory != null) {
                staffHistory = new HashSet<>();
                this.staffHistory.add(staffAssignmentHistory);
            }
        }
    }

    // StaffAssignmentHistory[assign staff]
    public void reassignStaff(final Staff newStaff, final LocalDate assignmentDate) {
        this.staff = newStaff;
        final StaffAssignmentHistory staffAssignmentHistory = StaffAssignmentHistory.builder()
            .center(this)
            .staff(this.staff)
            .startDate(assignmentDate.toDate())
            .build();
        this.staffHistory.add(staffAssignmentHistory);
    }

    // StaffAssignmentHistory[unassign staff]
    public void removeStaff(final LocalDate unassignDate) {
        this.staffHistory.stream()
            .filter(sah -> sah.getEndDate()==null)
            .findFirst().ifPresent(latestHistoryRecord -> latestHistoryRecord.setEndDate(unassignDate.toDate()));
    }

    private void setActivationDate(final Date activationDate, final AppUser loginUser, final List<ApiParameterError> dataValidationErrors) {

        if (activationDate != null) {
            this.activationDate = activationDate;
            this.activatedBy = loginUser;
        }

        validateActivationDate(dataValidationErrors);
   }

    private boolean isDateInTheFuture(final LocalDate localDate) {
        return localDate.isAfter(DateUtils.getLocalDateOfTenant());
    }

    private String hierarchyOf(final Long id) {
        return this.hierarchy + id.toString() + ".";
    }

    private void validateActivationDate(final List<ApiParameterError> dataValidationErrors) {

        if (getSubmittedOnDate() != null && isDateInTheFuture(getSubmittedOnDate())) {
            final String defaultUserMessage = "Submitted on date cannot be in the future.";
            final String globalisationMessageCode = "error.msg.group.submittedOnDate.in.the.future";
            final ApiParameterError error = ApiParameterError.parameterError(globalisationMessageCode, defaultUserMessage, GroupingTypesApiConstants.submittedOnDateParamName, this.submittedOnDate);

            dataValidationErrors.add(error);
        }

        if (getActivationLocalDate() != null && getSubmittedOnDate() != null && getSubmittedOnDate().isAfter(getActivationLocalDate())) {

            final String defaultUserMessage = "Submitted on date cannot be after the activation date";
            final ApiParameterError error = ApiParameterError.parameterError("error.msg.group.submittedOnDate.after.activation.date",
                defaultUserMessage, GroupingTypesApiConstants.submittedOnDateParamName, this.submittedOnDate);

            dataValidationErrors.add(error);
        }

        if (getActivationLocalDate() != null && isDateInTheFuture(getActivationLocalDate())) {

            final String defaultUserMessage = "Activation date cannot be in the future.";
            final ApiParameterError error = ApiParameterError.parameterError("error.msg.group.activationDate.in.the.future",
                defaultUserMessage, GroupingTypesApiConstants.activationDateParamName, getActivationLocalDate());

            dataValidationErrors.add(error);
        }

        if (getActivationLocalDate() != null) {
            if (this.office.getOpeningLocalDate().isAfter(getActivationLocalDate())) {
                final String defaultUserMessage = "Activation date cannot be a date before the office opening date.";
                final ApiParameterError error = ApiParameterError.parameterError("error.msg.group.activationDate.cannot.be.before.office.activation.date", defaultUserMessage, GroupingTypesApiConstants.activationDateParamName, getActivationLocalDate());
                dataValidationErrors.add(error);
            }
        }
    }

    private void validateStatusNotEqualToActiveAndLogError(final List<ApiParameterError> dataValidationErrors) {
        if (isActive()) {
            final String defaultUserMessage = "Cannot activate group. Group is already active.";
            final String globalisationMessageCode = "error.msg.group.already.active";
            final ApiParameterError error = ApiParameterError.parameterError(globalisationMessageCode, defaultUserMessage,
                GroupingTypesApiConstants.activeParamName, true);
            dataValidationErrors.add(error);
        }
    }
}