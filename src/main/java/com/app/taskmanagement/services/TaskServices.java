package com.app.taskmanagement.services;

import com.app.taskmanagement.entity.Employee;
import com.app.taskmanagement.entity.Projects;
import com.app.taskmanagement.entity.Task;
import com.app.taskmanagement.entity.User;
import com.app.taskmanagement.enums.Status;
import com.app.taskmanagement.repository.EmployeeRepository;
import com.app.taskmanagement.repository.ProjectRepository;
import com.app.taskmanagement.repository.TaskRepository;
import com.app.taskmanagement.requestdto.TaskRequestDto;
import com.app.taskmanagement.responsedto.AdminDashboardDto;
import com.app.taskmanagement.responsedto.ManagerTaskResponseDto;
import com.app.taskmanagement.responsedto.TaskResponseDto;
import com.app.taskmanagement.utils.Mapper;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * This class is the "brain" for everything to do with Tasks: creating them,
 * changing them, assigning them to people, and building every dashboard
 * (Manager, Employee, and Admin) that shows task counts.
 * <p>
 * Think of this class like a homework diary for a whole school: it knows
 * every assignment (Task), who it's given to, whether it's done yet, and
 * can hand back neat summaries like "how many assignments are still To-Do?"
 */
@Service
@RequiredArgsConstructor
public class TaskServices {
    private final TaskRepository taskRepository;
    private final EmployeeRepository employeeRepository;
    private final ProjectRepository projectRepository;
    private final ProjectServices projectServices;

    /**
     * Creates a brand new Task inside the CURRENTLY LOGGED-IN MANAGER's own project.
     * <p>
     * Simple example: like a teacher writing a new homework assignment on the
     * board for their own classroom - they can't accidentally create homework
     * for a classroom that isn't theirs, because we always use "their own"
     * project, never one picked from the request.
     * <p>
     * Steps: 1) find out who the logged-in manager is, 2) make sure they
     * don't already have a task with this exact title in their project
     * (no duplicates!), 3) make sure the due date isn't already in the past,
     * 4) save it.
     */
    @Transactional
    public TaskResponseDto addTask(TaskRequestDto taskRequestdto) {
        Employee manager = getCurrentUser();
        taskRepository.findByTitleAndProjects(taskRequestdto.getTitle(), manager.getProject().getId())
                .ifPresent(
                        (e) -> {
                            throw new RuntimeException("Task already Exist in Project!");
                        }
                );

        if (taskRequestdto.getDueDate().isBefore(LocalDate.now()))
            throw new RuntimeException("Deadline must be after " + LocalDate.now());
        Task t1 = Mapper.toTaskEntity(taskRequestdto);
        t1.setProjects(manager.getProject());
        t1.setStatus(Status.TO_DO);

        t1 = taskRepository.save(t1);
        return Mapper.toTaskReponseDto(t1);
    }

    /**
     * Updates a task's title/description/due date/priority - but ONLY if the
     * task actually belongs to the logged-in manager's own project.
     * <p>
     * Simple example: a teacher can only edit homework they themselves
     * assigned - not homework from a different classroom, even if they
     * somehow know its ID.
     */
    @Transactional
    public TaskResponseDto updateTask(TaskRequestDto dto, String id) {
        Task t1 = taskRepository.findById(id).orElseThrow(
                () -> new RuntimeException("Task doesn't exist!")
        );

        User manager = getCurrentUsernameAsUser();
        if (!t1.getProjects().getManager().getEmail().equals(manager.getEmail()))
            throw new RuntimeException("Task doesn't exist in your profile!");

        t1.setTitle(dto.getTitle());
        t1.setDescription(dto.getDescription());
        t1.setDueDate(dto.getDueDate());
        t1.setPriority(dto.getPriority());

        taskRepository.flush();
        return Mapper.toTaskReponseDto(t1);
    }

    /**
     * Fetches ONE task by its ID - but only lets the request through if the
     * task belongs to the logged-in manager's project.
     * <p>
     * Simple example: like asking "show me homework sheet #42" - the teacher
     * only gets shown the sheet if it's actually one they own.
     */
    public TaskResponseDto getTask(String id) {
        Task t1 = taskRepository.findById(id).orElseThrow(
                () -> new RuntimeException("Task doesn't exist!")
        );

        String currentUserEmail = getCurrentUsername();
        Employee emp = getCurrentUser(); // stays null if the caller is an Admin - and that's OK now, we guard it below

        boolean isManagerOfProject = t1.getProjects().getManager().getEmail().equals(currentUserEmail);
        boolean isAssignedEmployee = emp != null && emp.getTask() != null && emp.getTask().contains(t1);
        boolean isAdminOfCompany = t1.getProjects().getCompany().getAdmin().getEmail().equals(currentUserEmail);

        if (!isManagerOfProject && !isAssignedEmployee && !isAdminOfCompany)
            throw new RuntimeException("Task doesn't exist in your profile!");

        return Mapper.toTaskReponseDto(t1);
    }
    /**
     * Deletes a task - again, only if it belongs to the logged-in manager.
     * <p>
     * Simple example: you can only throw away homework sheets from your own
     * filing cabinet, not someone else's.
     */
    @Transactional
    public void deleteTask(String id) {
        Task t1 = taskRepository.findById(id).orElseThrow(
                () -> new RuntimeException("Task doesn't exist!")
        );

        User manager = getCurrentUsernameAsUser();
        if (!t1.getProjects().getManager().getEmail().equals(manager.getEmail()))
            throw new RuntimeException("Task doesn't exist in your profile!");

        taskRepository.delete(t1);
    }

    /**
     * Links one Employee to one Task, so that Employee now sees it on their
     * own task board.
     * <p>
     * Simple example: like pinning a copy of the homework sheet onto one
     * specific student's desk - now it shows up as "theirs" too.
     */
    @Transactional
    public String assignTaskToEmployee(String taskId, String employeeEmail) {
        Task t1 = taskRepository.findById(taskId).orElseThrow(() -> {
            throw new RuntimeException("Task doesn't found!");
        });
        Employee employee = employeeRepository.findByEmail(employeeEmail).orElseThrow(() -> new RuntimeException("Invalid Email"));

        employee.getTask().add(t1);
        t1.getEmployee().add(employee);
        employee.setProject(t1.getProjects());

        return "Task " + t1.getTitle() + "  is assigned successfully to " + employee.getName();
    }

    // ****************** Manager Dashboard ******************* //

    /**
     * Returns EVERY task inside the logged-in manager's own project, with an
     * extra "totalCommitments" count attached to each one (how many progress
     * notes employees have written on it so far).
     * <p>
     * Simple example: like a teacher's full gradebook page for their
     * classroom, with a little sticky-note count showing how many times
     * each student has already updated their progress on each assignment.
     */
    public List<ManagerTaskResponseDto> findAll() {
        Employee manager = getCurrentUser();
        List<Task> tasks = taskRepository.findTaskByProjects(manager.getProject().getId());

        return tasks.stream().map(task -> {
            TaskResponseDto baseDto = Mapper.toTaskReponseDto(task);
            return ManagerTaskResponseDto.builder()
                    .id(baseDto.getId())
                    .title(baseDto.getTitle())
                    .description(baseDto.getDescription())
                    .dueDate(baseDto.getDueDate())
                    .priority(baseDto.getPriority())
                    .status(baseDto.getStatus())
                    .projectName(baseDto.getProjectName())
                    .employee(baseDto.getEmployee())
                    .totalCommitments(task.getCommitments() != null ? task.getCommitments().size() : 0)
                    .build();
        }).collect(Collectors.toList());
    }

    /**
     * From the manager's own task list, picks out only the ones whose due
     * date has already passed AND that aren't marked Completed yet.
     * <p>
     * Simple example: like circling, in red pen, every homework assignment
     * that's now late.
     */
    public List<ManagerTaskResponseDto> findAllOverdueTask() {
        List<ManagerTaskResponseDto> overdueTask = new ArrayList<>();
        findAll().forEach(
                (e) -> {
                    if (e.getDueDate().isBefore(LocalDate.now()) && e.getStatus() != Status.COMPLETED)
                        overdueTask.add(e);
                }
        );
        return overdueTask;
    }

    /**
     * From the manager's own task list, picks out only the ones matching one
     * specific status (To-Do, In Progress, or Completed).
     * <p>
     * Simple example: like sorting a stack of homework sheets into three
     * separate piles - "not started," "in progress," and "done."
     */
    public List<ManagerTaskResponseDto> findAllTaskByStatus(Status status) {
        List<ManagerTaskResponseDto> statusTask = new ArrayList<>();
        findAll().forEach(
                (e) -> {
                    if (e.getStatus() == status) statusTask.add(e);
                }
        );
        return statusTask;
    }

    // ****************** Admin Dashboard (per-project) ******************* //

    /**
     * Builds the Admin's "report card" for ONE specific project: how many
     * managers/employees are on it, and how many tasks are in each status.
     * <p>
     * Simple example: like a school Principal asking for a one-page summary
     * of ONE classroom - "how many students, how much homework is done,
     * how much is overdue" - without needing to personally be that
     * classroom's teacher. This is different from a Manager's own dashboard,
     * because an Admin can own MANY projects, not just one, so we always
     * take the projectId as an explicit argument instead of guessing it
     * from "whoever is logged in".
     * <p>
     * Security check: makes sure the logged-in Admin actually owns the
     * company this project belongs to, so one Admin can't peek at another
     * company's numbers just by guessing a project ID.
     */
    public AdminDashboardDto getProjectDashboard(String projectId) {
        Projects project = projectRepository.findById(projectId).orElseThrow(
                () -> new RuntimeException("Project doesn't exist!")
        );

        if (!project.getCompany().getAdmin().getEmail().equals(getCurrentUsername()))
            throw new RuntimeException("Project doesn't exist!");

        List<Task> tasks = taskRepository.findTaskByProjects(projectId);
        long toDo = tasks.stream().filter(t -> t.getStatus() == Status.TO_DO).count();
        long inProgress = tasks.stream().filter(t -> t.getStatus() == Status.IN_PROGRESS).count();
        long completed = tasks.stream().filter(t -> t.getStatus() == Status.COMPLETED).count();
        long overdue = taskRepository.findOverdueTaskByProjects(projectId).size();

        Object employeeCountObj = employeeRepository.findTotalEmployeeInProject(projectId);
        int employeeCount = employeeCountObj != null ? ((Number) employeeCountObj).intValue() : 0;
        // The manager is ALSO stored with this same project id (see ProjectServices.addProjects),
        // so we subtract 1 to avoid counting the manager twice (once as "manager", once as "employee").
        boolean hasManager = project.getManager() != null;
        if (hasManager && employeeCount > 0) employeeCount = employeeCount - 1;

        return AdminDashboardDto.builder()
                .totalManager(hasManager ? 1 : 0)
                .totalEmployee(employeeCount)
                .toDoTask((int) toDo)
                .inProgressTask((int) inProgress)
                .completedTask((int) completed)
                .overDueTask((int) overdue)
                .deadline(project.getDeadline())
                .build();
    }

    /**
     * The Admin's drill-down into ONE status column for ONE project - e.g.
     * "show me every To-Do task in Project X", regardless of who's logged in
     * managing it.
     * <p>
     * Simple example: like the Principal asking "show me the actual list of
     * unfinished homework sheets in classroom 5B", not just the number "3".
     */
    public List<TaskResponseDto> getProjectTasksByStatus(String projectId, Status status) {
        verifyAdminOwnsProject(projectId);
        return taskRepository.findTaskByStatusAndProjects(status, projectId)
                .stream().map(Mapper::toTaskReponseDto).collect(Collectors.toList());
    }

    /**
     * The Admin's drill-down into the Overdue column for ONE project.
     * <p>
     * Simple example: like the Principal asking "show me exactly which
     * homework sheets in classroom 5B are late", not just the number.
     */
    public List<TaskResponseDto> getProjectOverdueTasks(String projectId) {
        verifyAdminOwnsProject(projectId);
        return taskRepository.findOverdueTaskByProjects(projectId)
                .stream().map(Mapper::toTaskReponseDto).collect(Collectors.toList());
    }

    private void verifyAdminOwnsProject(String projectId) {
        Projects project = projectRepository.findById(projectId).orElseThrow(
                () -> new RuntimeException("Project doesn't exist!")
        );
        if (!project.getCompany().getAdmin().getEmail().equals(getCurrentUsername()))
            throw new RuntimeException("Project doesn't exist!");
    }

    // ****************** Employee Dashboard ******************* //

    /**
     * Returns every task assigned to the currently logged-in Employee (or
     * Manager, since a Manager is a kind of Employee too), across however
     * many tasks they've been given.
     * <p>
     * Simple example: like a student flipping open their own personal
     * assignment folder - only THEIR homework shows up, nobody else's.
     */
    public List<TaskResponseDto> getTaskOfEmployee() {
        User employee = getCurrentUsernameAsUser();
        return taskRepository.findTaskOfEmployee(employee.getEmail()).stream().map(
                Mapper::toTaskReponseDto
        ).toList();
    }

    /**
     * Same as above, but filtered down to just one status - e.g. "only show
     * me MY tasks that are still To-Do."
     */
    public List<TaskResponseDto> getTaskOfEmployeeByStatus(Status status) {
        List<TaskResponseDto> statusTaskList = new ArrayList<>();
        getTaskOfEmployee().forEach(
                (e) -> {
                    if (e.getStatus() == status) statusTaskList.add(e);
                }
        );
        return statusTaskList;
    }

    /**
     * Marks a task as fully Completed - only a Manager can do this (enforced
     * in the Controller), and only for a task in their own project.
     * <p>
     * After marking it done, this also asks ProjectServices to re-check
     * whether the WHOLE PROJECT should now flip to "Completed" too (if every
     * single task in it is finished).
     * <p>
     * Simple example: like a teacher stamping "DONE" on one homework sheet,
     * which then makes the school's attendance office automatically check
     * "has this whole class finished everything for the term yet?"
     */
    @Transactional
    public void markItAsCompleted(String id) {
        User manager = getCurrentUsernameAsUser();
        Task t1 = taskRepository.findById(id).orElseThrow(
                () -> new RuntimeException("Invalid Task id.")
        );
        if (!t1.getProjects().getManager().getEmail().equals(manager.getEmail())) throw new RuntimeException(
                "Task doesn't Exist!"
        );
        t1.setStatus(Status.COMPLETED);
        projectServices.changeStatus(t1.getProjects().getId());
    }

    /**
     * Automatically flips a task from "To-Do" to "In Progress" the very
     * first time an employee writes a Commitment (progress note) on it.
     * This method is never called directly by a Controller - it's silently
     * triggered by CommitmentServices.addCommits() behind the scenes.
     * <p>
     * Simple example: a homework sheet doesn't need a student to manually
     * flip a switch saying "I started this" - the moment they write their
     * FIRST progress note on it, it visually moves itself into the
     * "in progress" pile automatically.
     */
    @Transactional
    public void changeStatusToIn_Progress(String id) {
        Task t1 = taskRepository.findById(id).orElseThrow(
                () -> new RuntimeException("Invalid Task Id.")
        );
        if (t1.getStatus() == Status.TO_DO) {
            t1.setStatus(Status.IN_PROGRESS);
            projectServices.changeStatus(t1.getProjects().getId());
        }
    }

    @Transactional
    public void changeStatusTo_ToDO(String id) {
        Task t1 = taskRepository.findById(id).orElseThrow(
                () -> new RuntimeException("Invalid Task Id.")
        );
        if (t1.getStatus() == Status.IN_PROGRESS) {
            t1.setStatus(Status.TO_DO);
            projectServices.changeStatus(t1.getProjects().getId());
        }
    }

    /**
     * A small helper that looks at "who is currently logged in" (from the
     * JWT wristband Spring Security already validated) and fetches their
     * full Employee record from the database.
     * <p>
     * Simple example: like checking the name tag on someone's wristband,
     * then looking up their full student file using that name.
     */
    private Employee getCurrentUser() {
        UserDetails userDetails = (UserDetails) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        try {
            return employeeRepository.findByEmail(userDetails.getUsername()).get();
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * Same idea as getCurrentUser(), but only reads the username string - no database trip.
     */
    private String getCurrentUsername() {
        return ((UserDetails) SecurityContextHolder.getContext().getAuthentication().getPrincipal()).getUsername();
    }

    /**
     * Builds a lightweight User object out of just the logged-in username,
     * used only for the ".getEmail()" ownership-comparison checks above.
     */
    private User getCurrentUsernameAsUser() {
        String username = getCurrentUsername();
        return User.builder().email(username).build();
    }
}
