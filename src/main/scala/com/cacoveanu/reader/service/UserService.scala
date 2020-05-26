package com.cacoveanu.reader.service

import java.util

import com.cacoveanu.reader.entity.DbUser
import com.cacoveanu.reader.repository.UserRepository
import javax.annotation.PostConstruct
import javax.persistence.{Column, Entity, GeneratedValue, GenerationType, Id}
import org.springframework.beans.factory.annotation.Autowired
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

class UserPrincipal(val user: DbUser) extends UserDetails {
  override def getAuthorities: util.Collection[_ <: GrantedAuthority] = {
    Seq(new ReaderAuthority("USER")).asJava
  }

  override def getPassword: String = user.password

  override def getUsername: String = user.username

  override def isAccountNonExpired: Boolean = true

  override def isAccountNonLocked: Boolean = true

  override def isCredentialsNonExpired: Boolean = true

  override def isEnabled: Boolean = true
}

@Service
class UserService extends UserDetailsService{

  @BeanProperty @Autowired var userRepository: UserRepository = _

  @BeanProperty @Autowired var passwordEncoder: PasswordEncoder = _

  @PostConstruct
  def defaultUser(): Unit = {
    val defaultUserName = "test"
    val existingUser = userRepository.findByUsername(defaultUserName)
    val user = new DbUser()
    if (existingUser != null) user.id = existingUser.id
    user.username = defaultUserName
    user.password = passwordEncoder.encode("test")
    userRepository.save(user)
  }

  def loadUser(username: String): Option[DbUser] = {
    val dbUser = userRepository.findByUsername(username)
    Option(dbUser)
  }

  override def loadUserByUsername(username: String): UserDetails = {
    val user = userRepository.findByUsername(username)
    if (user == null) throw new UsernameNotFoundException(username)
    new UserPrincipal(user)
  }
}
