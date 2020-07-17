package com.cacoveanu.reader.service

import java.util

import com.cacoveanu.reader.entity.Account
import com.cacoveanu.reader.repository.AccountRepository
import javax.annotation.PostConstruct
import org.springframework.beans.factory.annotation.{Autowired, Value}
import org.springframework.security.core.GrantedAuthority
import org.springframework.security.core.userdetails.{UserDetails, UserDetailsService}
import org.springframework.stereotype.Service
import org.springframework.security.core.userdetails.UsernameNotFoundException
import org.springframework.security.crypto.password.PasswordEncoder

import scala.beans.BeanProperty
import scala.jdk.CollectionConverters._

class ReaderAuthority(name: String) extends GrantedAuthority {
  override def getAuthority: String = name
}

class UserPrincipal(val user: Account) extends UserDetails {
  override def getAuthorities: util.Collection[_ <: GrantedAuthority] = {
    if (user.admin) {
      Seq(new ReaderAuthority("ROLE_ADMIN")).asJava
    } else {
      Seq(new ReaderAuthority("ROLE_USER")).asJava
    }
  }

  override def getPassword: String = user.password

  override def getUsername: String = user.username

  override def isAccountNonExpired: Boolean = true

  override def isAccountNonLocked: Boolean = true

  override def isCredentialsNonExpired: Boolean = true

  override def isEnabled: Boolean = true
}

@Service
class UserService extends UserDetailsService {

  @BeanProperty @Autowired var userRepository: AccountRepository = _

  @BeanProperty @Autowired var passwordEncoder: PasswordEncoder = _

  @Value("${adminPass}")
  @BeanProperty
  var adminPass: String = _

  @PostConstruct
  def defaultUser(): Unit = {
    val adminUsername = "admin"
    val existingUser = userRepository.findByUsername(adminUsername)
    val user = new Account()
    if (existingUser != null) user.id = existingUser.id
    user.username = adminUsername
    user.password = passwordEncoder.encode(adminPass)
    user.admin = true
    userRepository.save(user)
  }

  def loadUser(username: String): Option[Account] = {
    val dbUser = userRepository.findByUsername(username)
    Option(dbUser)
  }

  def changePassword(user: Account, oldPassword: String, newPassword: String): Boolean = {
    if (passwordEncoder.matches(oldPassword, user.password)) {
      user.password = passwordEncoder.encode(newPassword)
      userRepository.save(user)
      true
    } else {
      false
    }
  }

  override def loadUserByUsername(username: String): UserDetails = {
    val user = userRepository.findByUsername(username)
    if (user == null) throw new UsernameNotFoundException(username)
    new UserPrincipal(user)
  }
}
