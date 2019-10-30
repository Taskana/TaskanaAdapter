package pro.taskana.adapter.camunda.outbox.rest.springboot.controller;

import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import pro.taskana.adapter.camunda.outbox.rest.core.dto.ReferencedTaskDTO;

import java.util.List;

@RestController
@RequestMapping ( path = "taskana-listener-events/rest/outbox")
public interface OutboxRestServiceSpringBootRestController {

    @RequestMapping(path = "/getCreateEvents",method = RequestMethod.GET)
    List<ReferencedTaskDTO> getCreateEvents();

    @RequestMapping ( path = "/delete", method = RequestMethod.DELETE)
    void deleteEvents(@RequestParam ("ids") String ids);

}
