package io.kadai.adapter.impl;

import io.kadai.common.api.security.UserPrincipal;
import java.security.PrivilegedAction;
import java.util.function.Supplier;
import javax.security.auth.Subject;

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
