package io.choerodon.iam.api.controller.v1;

import io.choerodon.core.base.BaseController;
import io.choerodon.core.domain.Page;
import io.choerodon.core.iam.InitRoleCode;
import io.choerodon.core.iam.ResourceLevel;
import io.choerodon.core.validator.ValidList;
import io.choerodon.iam.api.dto.*;
import io.choerodon.iam.api.validator.MemberRoleValidator;
import io.choerodon.iam.api.validator.RoleAssignmentViewValidator;
import io.choerodon.iam.app.service.RoleMemberService;
import io.choerodon.iam.app.service.RoleService;
import io.choerodon.iam.app.service.UploadHistoryService;
import io.choerodon.iam.app.service.UserService;
import io.choerodon.iam.infra.enums.ExcelSuffix;
import io.choerodon.mybatis.pagehelper.annotation.SortDefault;
import io.choerodon.mybatis.pagehelper.domain.PageRequest;
import io.choerodon.mybatis.pagehelper.domain.Sort;
import io.choerodon.swagger.annotation.CustomPageRequest;
import io.choerodon.swagger.annotation.Permission;
import io.swagger.annotations.ApiOperation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import springfox.documentation.annotations.ApiIgnore;

import javax.validation.Valid;
import java.util.List;

/**
 * @author superlee
 * @author wuguokai
 */
@RestController
@RequestMapping(value = "/v1")
public class RoleMemberController extends BaseController {

    private RoleMemberService roleMemberService;

    private UserService userService;

    private RoleService roleService;
    private UploadHistoryService uploadHistoryService;

    @Autowired
    private MemberRoleValidator memberRoleValidator;

    public RoleMemberController(RoleMemberService roleMemberService,
                                UserService userService,
                                RoleService roleService,
                                UploadHistoryService uploadHistoryService) {
        this.roleMemberService = roleMemberService;
        this.userService = userService;
        this.roleService = roleService;
        this.uploadHistoryService = uploadHistoryService;
    }

    /**
     * 在site层分配角色
     * <p>
     * is_edit 是否是编辑，如果false就表示新建角色，true表示是在是编辑角色
     */
    @Permission(level = ResourceLevel.SITE)
    @ApiOperation(value = "全局层批量分配给用户角色")
    @PostMapping(value = "/site/role_members")
    public ResponseEntity<List<MemberRoleDTO>> createOrUpdateOnSiteLevel(@RequestParam(value = "is_edit", required = false) Boolean isEdit,
                                                                         @RequestParam(name = "member_ids") List<Long> memberIds,
                                                                         @RequestBody ValidList<MemberRoleDTO> memberRoleDTOList) {
        memberRoleValidator.distributionRoleValidator(ResourceLevel.SITE.value(), memberRoleDTOList);
        return new ResponseEntity<>(roleMemberService.createOrUpdateRolesByMemberIdOnSiteLevel(
                isEdit, memberIds, memberRoleDTOList), HttpStatus.OK);
    }

    /**
     * 在organization层分配角色
     */
    @Permission(level = ResourceLevel.ORGANIZATION)
    @ApiOperation(value = "组织层批量分配给用户角色")
    @PostMapping(value = "/organizations/{organization_id}/role_members")
    public ResponseEntity<List<MemberRoleDTO>> createOrUpdateOnOrganizationLevel(@RequestParam(value = "is_edit", required = false) Boolean isEdit,
                                                                                 @PathVariable(name = "organization_id") Long sourceId,
                                                                                 @RequestParam(name = "member_ids") List<Long> memberIds,
                                                                                 @RequestBody ValidList<MemberRoleDTO> memberRoleDTOList) {
        memberRoleValidator.distributionRoleValidator(ResourceLevel.ORGANIZATION.value(), memberRoleDTOList);
        return new ResponseEntity<>(roleMemberService.createOrUpdateRolesByMemberIdOnOrganizationLevel(
                isEdit, sourceId, memberIds, memberRoleDTOList), HttpStatus.OK);
    }

    /**
     * 在project层分配角色
     */
    @Permission(level = ResourceLevel.PROJECT, roles = InitRoleCode.PROJECT_OWNER)
    @ApiOperation(value = "项目层批量分配给用户角色")
    @PostMapping(value = "/projects/{project_id}/role_members")
    public ResponseEntity<List<MemberRoleDTO>> createOrUpdateOnProjectLevel(@RequestParam(value = "is_edit", required = false) Boolean isEdit,
                                                                            @PathVariable(name = "project_id") Long sourceId,
                                                                            @RequestParam(name = "member_ids") List<Long> memberIds,
                                                                            @RequestBody ValidList<MemberRoleDTO> memberRoleDTOList) {
        memberRoleValidator.distributionRoleValidator(ResourceLevel.PROJECT.value(), memberRoleDTOList);
        return new ResponseEntity<>(roleMemberService.createOrUpdateRolesByMemberIdOnProjectLevel(
                isEdit, sourceId, memberIds, memberRoleDTOList), HttpStatus.OK);
    }

    /**
     * 在site层根据成员id和角色id删除角色
     */
    @Permission(level = ResourceLevel.SITE)
    @ApiOperation(value = "全局层批量移除用户的角色")
    @PostMapping(value = "/site/role_members/delete")
    public ResponseEntity deleteOnSiteLevel(@RequestBody @Valid RoleAssignmentDeleteDTO roleAssignmentDeleteDTO) {
        RoleAssignmentViewValidator.validate(roleAssignmentDeleteDTO.getView());
        roleAssignmentDeleteDTO.setSourceId(0L);
        roleMemberService.deleteOnSiteLevel(roleAssignmentDeleteDTO);
        return new ResponseEntity(HttpStatus.NO_CONTENT);
    }

    /**
     * 在organization层根据成员id和角色id删除角色
     */
    @Permission(level = ResourceLevel.ORGANIZATION)
    @ApiOperation(value = "组织层批量移除用户的角色")
    @PostMapping(value = "/organizations/{organization_id}/role_members/delete")
    public ResponseEntity deleteOnOrganizationLevel(@PathVariable(name = "organization_id") Long sourceId,
                                                    @RequestBody @Valid RoleAssignmentDeleteDTO roleAssignmentDeleteDTO) {
        RoleAssignmentViewValidator.validate(roleAssignmentDeleteDTO.getView());
        roleAssignmentDeleteDTO.setSourceId(sourceId);
        roleMemberService.deleteOnOrganizationLevel(roleAssignmentDeleteDTO);
        return new ResponseEntity(HttpStatus.NO_CONTENT);
    }

    /**
     * 在project层根据id删除角色
     */
    @Permission(level = ResourceLevel.PROJECT, roles = InitRoleCode.PROJECT_OWNER)
    @ApiOperation(value = "项目层批量移除用户的角色")
    @PostMapping(value = "/projects/{project_id}/role_members/delete")
    public ResponseEntity deleteOnProjectLevel(@PathVariable(name = "project_id") Long sourceId,
                                               @RequestBody @Valid RoleAssignmentDeleteDTO roleAssignmentDeleteDTO) {
        RoleAssignmentViewValidator.validate(roleAssignmentDeleteDTO.getView());
        roleAssignmentDeleteDTO.setSourceId(sourceId);
        roleMemberService.deleteOnProjectLevel(roleAssignmentDeleteDTO);
        return new ResponseEntity(HttpStatus.NO_CONTENT);
    }

    /**
     * 根据角色Id分页查询该角色被分配的用户
     *
     * @param pageRequest
     * @param roleAssignmentSearchDTO
     * @return
     */
    @Permission(level = ResourceLevel.SITE)
    @ApiOperation(value = "全局层分页查询角色下的用户")
    @CustomPageRequest
    @PostMapping(value = "/site/role_members/users")
    public ResponseEntity<Page<UserDTO>> pagingQueryUsersByRoleIdOnSiteLevel(
            @ApiIgnore
            @SortDefault(value = "id", direction = Sort.Direction.ASC) PageRequest pageRequest,
            @RequestParam(name = "role_id") Long roleId,
            @RequestBody(required = false) @Valid RoleAssignmentSearchDTO roleAssignmentSearchDTO) {
        return new ResponseEntity<>(userService.pagingQueryUsersByRoleIdOnSiteLevel(
                pageRequest, roleAssignmentSearchDTO, roleId), HttpStatus.OK);
    }

    @Permission(level = ResourceLevel.ORGANIZATION)
    @ApiOperation(value = "组织层分页查询角色下的用户")
    @CustomPageRequest
    @PostMapping(value = "/organizations/{organization_id}/role_members/users")
    public ResponseEntity<Page<UserDTO>> pagingQueryUsersByRoleIdOnOrganizationLevel(
            @ApiIgnore
            @SortDefault(value = "id", direction = Sort.Direction.ASC) PageRequest pageRequest,
            @RequestParam(name = "role_id") Long roleId,
            @PathVariable(name = "organization_id") Long sourceId,
            @RequestBody(required = false) @Valid RoleAssignmentSearchDTO roleAssignmentSearchDTO) {
        return new ResponseEntity<>(userService.pagingQueryUsersByRoleIdOnOrganizationLevel(
                pageRequest, roleAssignmentSearchDTO, roleId, sourceId), HttpStatus.OK);
    }

    @Permission(level = ResourceLevel.PROJECT, roles = InitRoleCode.PROJECT_OWNER)
    @ApiOperation(value = "项目层分页查询角色下的用户")
    @CustomPageRequest
    @PostMapping(value = "/projects/{project_id}/role_members/users")
    public ResponseEntity<Page<UserDTO>> pagingQueryUsersByRoleIdOnProjectLevel(
            @ApiIgnore
            @SortDefault(value = "id", direction = Sort.Direction.ASC) PageRequest pageRequest,
            @RequestParam(name = "role_id") Long roleId,
            @PathVariable(name = "project_id") Long sourceId,
            @RequestBody(required = false) @Valid RoleAssignmentSearchDTO roleAssignmentSearchDTO) {
        return new ResponseEntity<>(userService.pagingQueryUsersByRoleIdOnProjectLevel(
                pageRequest, roleAssignmentSearchDTO, roleId, sourceId), HttpStatus.OK);
    }

    /**
     * 查询site层角色,附带该角色下分配的用户数
     *
     * @return 查询结果
     */
    @Permission(level = ResourceLevel.SITE)
    @ApiOperation(value = "全局层查询角色列表以及该角色下的用户数量")
    @PostMapping(value = "/site/role_members/users/count")
    public ResponseEntity<List<RoleDTO>> listRolesWithUserCountOnSiteLevel(
            @RequestBody(required = false) @Valid RoleAssignmentSearchDTO roleAssignmentSearchDTO) {
        return new ResponseEntity<>(roleService.listRolesWithUserCountOnSiteLevel(
                roleAssignmentSearchDTO), HttpStatus.OK);
    }

    /**
     * 查询organization层角色,附带该角色下分配的用户数
     *
     * @return 查询结果
     */
    @Permission(level = ResourceLevel.ORGANIZATION)
    @ApiOperation(value = "组织层查询角色列表以及该角色下的用户数量")
    @PostMapping(value = "/organizations/{organization_id}/role_members/users/count")
    public ResponseEntity<List<RoleDTO>> listRolesWithUserCountOnOrganizationLevel(
            @PathVariable(name = "organization_id") Long sourceId,
            @RequestBody(required = false) @Valid RoleAssignmentSearchDTO roleAssignmentSearchDTO) {
        return new ResponseEntity<>(roleService.listRolesWithUserCountOnOrganizationLevel(
                roleAssignmentSearchDTO, sourceId), HttpStatus.OK);
    }

    /**
     * 查询project层角色,附带该角色下分配的用户数
     *
     * @return 查询结果
     */
    @Permission(level = ResourceLevel.PROJECT, roles = InitRoleCode.PROJECT_OWNER)
    @ApiOperation(value = "项目层查询角色列表以及该角色下的用户数量")
    @PostMapping(value = "/projects/{project_id}/role_members/users/count")
    public ResponseEntity<List<RoleDTO>> listRolesWithUserCountOnProjectLevel(
            @PathVariable(name = "project_id") Long sourceId,
            @RequestBody(required = false) @Valid RoleAssignmentSearchDTO roleAssignmentSearchDTO) {
        return new ResponseEntity<>(roleService.listRolesWithUserCountOnProjectLevel(
                roleAssignmentSearchDTO, sourceId), HttpStatus.OK);
    }

    /**
     * 在site层查询用户，用户包含拥有的site层的角色
     *
     * @param roleAssignmentSearchDTO 搜索条件
     */
    @Permission(level = ResourceLevel.SITE)
    @ApiOperation(value = "全局层查询用户列表以及该用户拥有的角色")
    @CustomPageRequest
    @PostMapping(value = "/site/role_members/users/roles")
    public ResponseEntity<Page<UserWithRoleDTO>> pagingQueryUsersWithSiteLevelRoles(
            @ApiIgnore
            @SortDefault(value = "id", direction = Sort.Direction.ASC) PageRequest pageRequest,
            @RequestBody(required = false) @Valid RoleAssignmentSearchDTO roleAssignmentSearchDTO) {
        return new ResponseEntity<>(userService.pagingQueryUsersWithSiteLevelRoles(
                pageRequest, roleAssignmentSearchDTO), HttpStatus.OK);
    }

    /**
     * 在site层查询用户，用户包含拥有的organization层的角色
     *
     * @param roleAssignmentSearchDTO 搜索条件
     */
    @Permission(level = ResourceLevel.ORGANIZATION)
    @ApiOperation(value = "组织层查询用户列表以及该用户拥有的角色")
    @CustomPageRequest
    @PostMapping(value = "/organizations/{organization_id}/role_members/users/roles")
    public ResponseEntity<Page<UserWithRoleDTO>> pagingQueryUsersWithOrganizationLevelRoles(
            @ApiIgnore
            @SortDefault(value = "id", direction = Sort.Direction.ASC) PageRequest pageRequest,
            @PathVariable(name = "organization_id") Long sourceId,
            @RequestBody(required = false) @Valid RoleAssignmentSearchDTO roleAssignmentSearchDTO) {
        return new ResponseEntity<>(userService.pagingQueryUsersWithOrganizationLevelRoles(
                pageRequest, roleAssignmentSearchDTO, sourceId), HttpStatus.OK);
    }

    /**
     * 在site层查询用户，用户包含拥有的project层的角色
     *
     * @param roleAssignmentSearchDTO 搜索条件
     */
    @Permission(level = ResourceLevel.PROJECT, roles = InitRoleCode.PROJECT_OWNER)
    @ApiOperation(value = "项目层查询用户列表以及该用户拥有的角色")
    @CustomPageRequest
    @PostMapping(value = "/projects/{project_id}/role_members/users/roles")
    public ResponseEntity<Page<UserWithRoleDTO>> pagingQueryUsersWithProjectLevelRoles(
            @ApiIgnore
            @SortDefault(value = "id", direction = Sort.Direction.ASC) PageRequest pageRequest,
            @PathVariable(name = "project_id") Long sourceId,
            @RequestBody(required = false) @Valid RoleAssignmentSearchDTO roleAssignmentSearchDTO) {
        return new ResponseEntity<>(userService.pagingQueryUsersWithProjectLevelRoles(
                pageRequest, roleAssignmentSearchDTO, sourceId), HttpStatus.OK);
    }

    /**
     * 全局层下载模板
     *
     * @return
     */
    @Permission(level = ResourceLevel.SITE)
    @ApiOperation(value = "全局层下载excel导入模板")
    @GetMapping(value = "/site/role_members/download_templates")
    public ResponseEntity<Resource> downloadTemplatesOnSite() {
        return roleMemberService.downloadTemplates(ExcelSuffix.XLSX.value());
    }

    /**
     * 组织层下载模板
     *
     * @param organizationId
     * @return
     */
    @Permission(level = ResourceLevel.ORGANIZATION)
    @ApiOperation(value = "组织层下载excel导入模板")
    @GetMapping(value = "/organizations/{organization_id}/role_members/download_templates")
    public ResponseEntity<Resource> downloadTemplatesOnOrganization(@PathVariable(name = "organization_id") Long organizationId) {
        return roleMemberService.downloadTemplates(ExcelSuffix.XLSX.value());
    }

    /**
     * 组织层下载模板
     *
     * @param projectId
     * @return
     */
    @Permission(level = ResourceLevel.PROJECT)
    @ApiOperation(value = "项目层下载excel导入模板")
    @GetMapping(value = "/projects/{project_id}/role_members/download_templates")
    public ResponseEntity<Resource> downloadTemplatesOnProject(@PathVariable(name = "project_id") Long projectId) {
        return roleMemberService.downloadTemplates(ExcelSuffix.XLSX.value());
    }

    @Permission(level = ResourceLevel.SITE)
    @ApiOperation("site层从excel里面批量导入用户角色关系")
    @PostMapping("/site/role_members/batch_import")
    public ResponseEntity import2MemberRoleOnSite(@RequestPart MultipartFile file) {
        roleMemberService.import2MemberRole(0L, ResourceLevel.SITE.value(),file);
        return new ResponseEntity(HttpStatus.NO_CONTENT);
    }

    @Permission(level = ResourceLevel.ORGANIZATION)
    @ApiOperation("组织层从excel里面批量导入用户角色关系")
    @PostMapping("/organizations/{organization_id}/role_members/batch_import")
    public ResponseEntity import2MemberRoleOnOrganization(@PathVariable(name = "organization_id") Long organizationId,
                                                          @RequestPart MultipartFile file) {
        roleMemberService.import2MemberRole(organizationId, ResourceLevel.ORGANIZATION.value(),file);
        return new ResponseEntity(HttpStatus.NO_CONTENT);
    }

    @Permission(level = ResourceLevel.PROJECT)
    @ApiOperation("项目层从excel里面批量导入用户角色关系")
    @PostMapping("/projects/{project_id}/role_members/batch_import")
    public ResponseEntity import2MemberRoleOnProject(@PathVariable(name = "project_id") Long projectId,
                                                     @RequestPart MultipartFile file) {
        roleMemberService.import2MemberRole(projectId, ResourceLevel.PROJECT.value(),file);
        return new ResponseEntity(HttpStatus.NO_CONTENT);
    }

    @Permission(level = ResourceLevel.SITE)
    @ApiOperation("查site层的历史")
    @GetMapping("/site/member_role/upload/history")
    public ResponseEntity latestHistoryOnSite(@RequestParam(value = "user_id") Long userId) {
        return new ResponseEntity<>(uploadHistoryService.latestHistory(userId, "member-role", 0L,ResourceLevel.SITE.value()), HttpStatus.OK);
    }

    @Permission(level = ResourceLevel.ORGANIZATION)
    @ApiOperation("查组织层的历史")
    @GetMapping("/organizations/{organization_id}/member_role/upload/history")
    public ResponseEntity latestHistoryOnOrganization(@PathVariable(name = "organization_id") Long organizationId,
                                                      @RequestParam(value = "user_id") Long userId) {
        return new ResponseEntity<>(uploadHistoryService.latestHistory(userId, "member-role", organizationId, ResourceLevel.ORGANIZATION.value()), HttpStatus.OK);
    }

    @Permission(level = ResourceLevel.PROJECT)
    @ApiOperation("查项目层的历史")
    @GetMapping("/projects/{project_id}/member_role/upload/history")
    public ResponseEntity latestHistoryOnProject(@PathVariable(name = "project_id") Long projectId,
                                                 @RequestParam(value = "user_id") Long userId) {
        return new ResponseEntity<>(uploadHistoryService.latestHistory(userId, "member-role", projectId, ResourceLevel.PROJECT.value()), HttpStatus.OK);
    }

}
