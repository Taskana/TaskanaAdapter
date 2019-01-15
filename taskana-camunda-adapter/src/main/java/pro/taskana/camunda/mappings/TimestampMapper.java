package pro.taskana.camunda.mappings;

import java.time.Instant;
import java.util.List;

import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Result;
import org.apache.ibatis.annotations.Results;
import org.apache.ibatis.annotations.Select;

/**
 * Mapper for the Timestamps of the last creation Taskana tasks or completion of Camunda tasks.
 *
 * @author kkl
 */
@Mapper
public interface TimestampMapper {

    @Select("<script>"
        + "SELECT MAX(QUERY_TIMESTAMP) "
        + "FROM LAST_QUERY_TIME "
        + "WHERE CAMUNDA_SYSTEM_URL = #{camundaSystemUrl}"
        + "</script>")
    Instant getLatestQueryTimestamp(@Param("camundaSystemUrl") String camundaSystemUrl);

    @Select("<script>"
        + "SELECT MAX(CREATED) "
        + "FROM TASKS_CREATED "
        + "WHERE CAMUNDA_SYSTEM_URL = #{camundaSystemUrl}"
        + "</script>")
    Instant getLatestCreatedTaskCreationTimestamp(@Param("camundaSystemUrl") String camundaSystemUrl);

    @Select("<script>"
        + "SELECT ID "
        + "FROM TASKS_CREATED "
        + "WHERE (CAMUNDA_SYSTEM_URL = #{camundaSystemUrl} "
        + "AND  ID IN (<foreach item='item' collection='taskIdsIn' separator=',' >#{item}</foreach>))"
        + "</script>")
    @Results(value = {@Result(property = "taskId", column = "ID")})
    List<String> findExistingTaskIds(@Param("camundaSystemUrl") String camundaSystemUrl, @Param("taskIdsIn") List<String> taskIdsIn);

    @Select("<script>"
        + "SELECT ID "
        + "FROM TASKS_COMPLETED "
        + "WHERE ID IN (<foreach item='item' collection='taskIdsIn' separator=',' >#{item}</foreach>)"
        + "</script>")
    @Results(value = {@Result(property = "taskId", column = "ID")})
    List<String> findAlreadyCompletedTaskIds(@Param("taskIdsIn") List<String> taskIdsIn);

    @Insert("INSERT INTO TASKS_CREATED (ID, CREATED, CAMUNDA_SYSTEM_URL) VALUES (#{id}, #{created}, #{camundaSystemUrl})")
    void registerCreatedTask(@Param("id") String id,
        @Param("created") Instant created,
        @Param("camundaSystemUrl") String camundaSystemUrl);

    @Insert("INSERT INTO LAST_QUERY_TIME (ID, QUERY_TIMESTAMP, CAMUNDA_SYSTEM_URL) VALUES (#{id}, #{queryTimestamp}, #{camundaSystemUrl})")
    void rememberCamundaQueryTime(@Param("id") String id,
        @Param("queryTimestamp") Instant queryTimestamp,
        @Param("camundaSystemUrl") String camundaSystemUrl);

    @Delete("<script>"
        + "DELETE FROM TASKS_CREATED "
        + "WHERE CAMUNDA_SYSTEM_URL = #{camundaSystemUrl}"
        + "</script>")
    void removeLatestCreatedTimestamp(@Param("camundaSystemUrl") String camundaSystemUrl);

    @Select("<script>SELECT MAX(COMPLETED) FROM TASKS_COMPLETED </script>")
    Instant getLatestCompletedTimestamp();

    @Insert("INSERT INTO TASKS_COMPLETED (ID, COMPLETED, CAMUNDA_SYSTEM_URL) VALUES (#{id}, #{completed}, #{camundaSystemUrl})")
    void registerTaskCompleted(@Param("id") String id,
        @Param("completed") Instant completed,
        @Param("camundaSystemUrl") String camundaSystemUrl);

    @Delete("<script>DELETE TASKS_COMPLETED</script>")
    void clearCompletedTable();

}
