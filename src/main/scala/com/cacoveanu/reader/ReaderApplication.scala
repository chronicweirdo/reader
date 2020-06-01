package com.cacoveanu.reader

import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.SpringApplication
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.cache.annotation.EnableCaching
import org.springframework.context.annotation.{Bean, Configuration, Description}
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder
import org.springframework.security.config.annotation.web.builders.HttpSecurity
import org.springframework.security.config.annotation.web.configuration.{EnableWebSecurity, WebSecurityConfigurerAdapter}
import org.springframework.security.core.userdetails.UserDetailsService
import org.springframework.security.core.userdetails.User
import org.springframework.security.core.userdetails.UserDetails
import org.springframework.security.provisioning.InMemoryUserDetailsManager
import org.springframework.security.web.util.matcher.AntPathRequestMatcher
import org.springframework.context.annotation.Bean
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.context.annotation.Bean
import org.springframework.scheduling.annotation.EnableScheduling
import org.springframework.security.authentication.dao.DaoAuthenticationProvider
import org.springframework.security.core.userdetails.UserDetailsService
import org.springframework.security.crypto.password.PasswordEncoder

import scala.beans.BeanProperty

@SpringBootApplication
@EnableCaching
@EnableScheduling
class ReaderApplication {

}

object ReaderApplication extends App {
  SpringApplication.run(classOf[ReaderApplication])
}

import org.springframework.context.annotation.Configuration
import org.springframework.web.servlet.config.annotation.ViewControllerRegistry
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer

@Configuration
class MvcConfig extends WebMvcConfigurer {
  override def addViewControllers(registry: ViewControllerRegistry): Unit = {
    //registry.addViewController("/home").setViewName("home")
    //registry.addViewController("/").setViewName("collection")
    //registry.addViewController("/hello").setViewName("hello")
    registry.addViewController("/login").setViewName("login")
    registry.addRedirectViewController("/logout", "/login")
  }
}

@Configuration
@EnableWebSecurity
class WebSecurityConfig extends WebSecurityConfigurerAdapter {

  @Autowired
  var userService: UserDetailsService = _

  override def configure(http: HttpSecurity): Unit = {
    http
      .csrf().disable().cors().and()
      .authorizeRequests()
      .antMatchers("/testPost", "/testGet").permitAll()
      .antMatchers("/tail.png").permitAll()
      .anyRequest().authenticated()

      .and()
      .rememberMe().key("uniqueAndSecret")
      .userDetailsService(userService)

      .and()
      .formLogin()
      .loginPage("/login")
      .failureUrl("/login?error")
      //.successForwardUrl("/collection")
      .permitAll()

      .and()
      .logout()
      .logoutRequestMatcher(new AntPathRequestMatcher("/logout"))
      .logoutSuccessUrl("/login?logout")
      .deleteCookies("JSESSIONID")
      .permitAll()
  }



  /*@Bean
  @Autowired def authenticationProvider(passwordEncoder: PasswordEncoder, userDetailsService: UserDetailsService): DaoAuthenticationProvider = {
    val daoAuthenticationProvider = new DaoAuthenticationProvider
    daoAuthenticationProvider.setPasswordEncoder(passwordEncoder)
    daoAuthenticationProvider.setUserDetailsService(userDetailsService)
    daoAuthenticationProvider
  }*/

  @Bean def passwordEncoder = new BCryptPasswordEncoder

  /*@Bean
  override def userDetailsService(): UserDetailsService = {
    val user = User.withDefaultPasswordEncoder
      .username("user")
      .password("password")
      .roles("USER")
      .build

    new InMemoryUserDetailsManager(user)
  }*/
}