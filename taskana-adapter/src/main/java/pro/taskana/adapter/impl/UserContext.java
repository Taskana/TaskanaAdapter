package pro.taskana.adapter.impl;

import java.security.PrivilegedAction;
import java.util.function.Supplier;
import javax.security.auth.Subject;

import pro.taskana.common.internal.security.UserPrincipal;

public class UserContext {

  private UserContext() {
    throw new IllegalStateException("Utility class");
  }

  static <T> T runAsUser(String runAsUserId, Supplier<T> supplier) {
    Subject subject = new Subject();
    subject.getPrincipals().add(new UserPrincipal(runAsUserId));
    return Subject.doAs(subject, (PrivilegedAction<T>) supplier::get);
  }
}
