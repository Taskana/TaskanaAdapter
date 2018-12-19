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
        + "SELECT MAX(CREATED) "
        + "FROM TASKS_CREATED "
        + "WHERE CAMUNDA_SYSTEM_NAME = #{camundaSystemName}"
        + "</script>")
    Instant getLatestCreatedTimestamp(@Param("camundaSystemName") String camundaSystemName);

    @Select("<script>"
        + "SELECT ID "
        + "FROM TASKS_CREATED "
        + "WHERE ID IN (<foreach item='item' collection='taskIdsIn' separator=',' >#{item}</foreach>)"
        + "</script>")
    @Results(value = {@Result(property = "taskId", column = "ID")})
    List<String> findExistingTaskIds(@Param("camundaSystemName") String camundaSystemName, @Param("taskIdsIn") List<String> taskIdsIn);

    @Select("<script>"
        + "SELECT ID "
        + "FROM TASKS_COMPLETED "
        + "WHERE ID IN (<foreach item='item' collection='taskIdsIn' separator=',' >#{item}</foreach>)"
        + "</script>")
    @Results(value = {@Result(property = "taskId", column = "ID")})
    List<String> findAlreadyCompletedTaskIds(@Param("taskIdsIn") List<String> taskIdsIn);

    @Insert("INSERT INTO TASKS_CREATED (ID, CREATED, CAMUNDA_SYSTEM_NAME) VALUES (#{id}, #{created}, #{camundaSystemName})")
    void insertCreatedTask(@Param("id") String id,
        @Param("created") Instant created,
        @Param("camundaSystemName") String camundaSystemName);

    @Delete("<script>"
        + "DELETE FROM TASKS_CREATED "
        + "WHERE CAMUNDA_SYSTEM_NAME = #{camundaSystemName}"
        + "</script>")
    void removeLatestCreatedTimestamp(@Param("camundaSystemName") String camundaSystemName);

    @Select("<script>SELECT MAX(COMPLETED) FROM TASKS_COMPLETED </script>")
    Instant getLatestCompletedTimestamp();

    @Insert("INSERT INTO TASKS_COMPLETED (ID, COMPLETED) VALUES (#{id}, #{completed})")
    void insertCompletedTimestamp(@Param("id") String id,
        @Param("completed") Instant completed);

    @Delete("<script>DELETE TASKS_COMPLETED</script>")
    void clearCompletedTable();

}
