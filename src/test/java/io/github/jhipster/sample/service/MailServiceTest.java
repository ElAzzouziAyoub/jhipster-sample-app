package io.github.jhipster.sample.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

import io.github.jhipster.sample.IntegrationTest;
import io.github.jhipster.sample.domain.User;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import java.nio.charset.StandardCharsets;
import java.util.Locale;
import org.apache.commons.lang3.RandomStringUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.MessageSource;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.thymeleaf.spring6.SpringTemplateEngine;
import tech.jhipster.config.JHipsterProperties;

/**
 * Unit tests for the {@link MailService}.
 */
@IntegrationTest
class MailServiceTest {

    @Autowired
    private MailService mailService;

    @MockBean
    private JavaMailSender javaMailSender;

    @MockBean
    private MessageSource messageSource;

    @MockBean
    private SpringTemplateEngine templateEngine;

    @MockBean
    private JHipsterProperties jHipsterProperties;

    @MockBean
    private JHipsterProperties.Mail mailProperties;

    private User user;
    private MimeMessage mimeMessage;

    @BeforeEach
    void setUp() throws Exception {
        user = new User();
        user.setLogin("testuser");
        user.setEmail("testuser@example.com");
        user.setLangKey("en");
        user.setFirstName("Test");
        user.setLastName("User");

        mimeMessage = mock(MimeMessage.class);
        when(javaMailSender.createMimeMessage()).thenReturn(mimeMessage);
        when(jHipsterProperties.getMail()).thenReturn(mailProperties);
        when(mailProperties.getFrom()).thenReturn("test@example.com");
        when(mailProperties.getBaseUrl()).thenReturn("http://localhost:8080");

        // Mock cache properties to prevent NullPointerException in CacheConfiguration
        JHipsterProperties.Cache cacheProperties = mock(JHipsterProperties.Cache.class);
        JHipsterProperties.Cache.Ehcache ehcacheProperties = mock(JHipsterProperties.Cache.Ehcache.class);
        when(jHipsterProperties.getCache()).thenReturn(cacheProperties);
        when(cacheProperties.getEhcache()).thenReturn(ehcacheProperties);
        when(ehcacheProperties.getMaxEntries()).thenReturn(100L);
        when(ehcacheProperties.getTimeToLiveSeconds()).thenReturn(3600);

        when(messageSource.getMessage(anyString(), any(), any(Locale.class))).thenReturn("Test Subject");
        when(templateEngine.process(anyString(), any())).thenReturn("<html>Test Content</html>");
    }

    @Test
    void testSendEmail() throws Exception {
        mailService.sendEmail("test@example.com", "Test Subject", "Test Content", false, false);

        verify(javaMailSender, times(1)).createMimeMessage();
        verify(javaMailSender, times(1)).send(any(MimeMessage.class));
    }

    @Test
    void testSendEmailWithMultipart() throws Exception {
        mailService.sendEmail("test@example.com", "Test Subject", "Test Content", true, false);

        verify(javaMailSender, times(1)).createMimeMessage();
        verify(javaMailSender, times(1)).send(any(MimeMessage.class));
    }

    @Test
    void testSendEmailWithHtml() throws Exception {
        mailService.sendEmail("test@example.com", "Test Subject", "<html>Test</html>", false, true);

        verify(javaMailSender, times(1)).createMimeMessage();
        verify(javaMailSender, times(1)).send(any(MimeMessage.class));
    }

    @Test
    void testSendEmailFromTemplate() throws Exception {
        mailService.sendEmailFromTemplate(user, "mail/activationEmail", "email.activation.title");

        verify(templateEngine, times(1)).process(eq("mail/activationEmail"), any());
        verify(messageSource, times(1)).getMessage(eq("email.activation.title"), any(), eq(Locale.forLanguageTag("en")));
        verify(javaMailSender, times(1)).send(any(MimeMessage.class));
    }

    @Test
    void testSendEmailFromTemplateWithNullEmail() throws Exception {
        user.setEmail(null);

        mailService.sendEmailFromTemplate(user, "mail/activationEmail", "email.activation.title");

        verify(templateEngine, never()).process(anyString(), any());
        verify(javaMailSender, never()).send(any(MimeMessage.class));
    }

    @Test
    void testSendActivationEmail() throws Exception {
        mailService.sendActivationEmail(user);

        verify(templateEngine, times(1)).process(eq("mail/activationEmail"), any());
        verify(messageSource, times(1)).getMessage(eq("email.activation.title"), any(), any(Locale.class));
        verify(javaMailSender, times(1)).send(any(MimeMessage.class));
    }

    @Test
    void testSendCreationEmail() throws Exception {
        mailService.sendCreationEmail(user);

        verify(templateEngine, times(1)).process(eq("mail/creationEmail"), any());
        verify(messageSource, times(1)).getMessage(eq("email.activation.title"), any(), any(Locale.class));
        verify(javaMailSender, times(1)).send(any(MimeMessage.class));
    }

    @Test
    void testSendPasswordResetMail() throws Exception {
        mailService.sendPasswordResetMail(user);

        verify(templateEngine, times(1)).process(eq("mail/passwordResetEmail"), any());
        verify(messageSource, times(1)).getMessage(eq("email.reset.title"), any(), any(Locale.class));
        verify(javaMailSender, times(1)).send(any(MimeMessage.class));
    }

    @Test
    void testSendEmailHandlesMessagingException() throws Exception {
        doThrow(new MessagingException("Test exception")).when(javaMailSender).send(any(MimeMessage.class));

        // Should not throw exception, just log
        mailService.sendEmail("test@example.com", "Test Subject", "Test Content", false, false);

        verify(javaMailSender, times(1)).send(any(MimeMessage.class));
    }

    @Test
    void testSendEmailHandlesMailException() throws Exception {
        doThrow(new org.springframework.mail.MailException("Test exception") {}).when(javaMailSender).send(any(MimeMessage.class));

        // Should not throw exception, just log
        mailService.sendEmail("test@example.com", "Test Subject", "Test Content", false, false);

        verify(javaMailSender, times(1)).send(any(MimeMessage.class));
    }
}
