package eu.innovation.engineering.dataset.main;

import java.util.Arrays;

import org.springframework.beans.factory.support.DefaultListableBeanFactory;
import org.springframework.context.ApplicationContext;
import org.springframework.context.support.ClassPathXmlApplicationContext;
import org.springframework.context.support.GenericApplicationContext;

import eu.innovationengineering.solrclient.auth.utils.function.ConsumerWithException;

public class SpringMainLauncher {

  protected static void mainWithSpring(ConsumerWithException<ApplicationContext, Exception> body, String[] args, String... configLocations) throws Exception {
    DefaultListableBeanFactory beanFactory = new DefaultListableBeanFactory();
    // Define a bean and register it
    beanFactory.registerSingleton("args", Arrays.asList(args));
    try (GenericApplicationContext cmdArgCxt = new GenericApplicationContext(beanFactory)) {
      // Must call refresh to initialize context
      cmdArgCxt.refresh();
      
      try (ClassPathXmlApplicationContext context = new ClassPathXmlApplicationContext(configLocations, true, cmdArgCxt)) {
        body.acceptWithException(context);
      }
      
    }
  }
  
}
