package uk.co.anthonycampbell.grails.contactform

import grails.test.mixin.TestFor
import grails.test.mixin.TestMixin
import org.junit.Before
import grails.test.mixin.domain.DomainClassUnitTestMixin
import org.grails.plugin.jcaptcha.JcaptchaService
import grails.plugin.mail.MailService

/**
 * Set of unit tests for the contact form controller.
 */
@TestFor(ContactFormController)
@TestMixin(DomainClassUnitTestMixin)
class ContactFormControllerTests {

    // Declare test properties
    ContactFormController controller
    def mockJcaptchaService
    def mockMailService
    def mockedConfig
    
    def validProperties
    def emptyProperties

    /**
     * Initialise test parameters and controller
     */
    @Before
    void setUp() {
        // Mock dependencies
        mockJcaptchaService = mockFor(JcaptchaService.class)
        mockMailService = mockFor(MailService.class)

        // Initialise controller
        controller = ContactFormController.newInstance()
        mockedConfig = ConfigObject.newInstance()

        // Add message lookup to always return key
        controller.metaClass.message = { Map args -> return args.code }

        // Initialise test form properties
        validProperties = [yourFullName: "Joe Bloggs",
            yourEmailAddress: "joe@bloggs.com",
            subject: "Joe",
            message: "Bloggs",
            captcha: "123456"]
        emptyProperties = [yourFullName: "",
            yourEmailAddress: "",
            subject: "",
            message: "",
            captcha: ""]
    }

    void testAllowedMethodsSize() {
        assertEquals "Unexpected number of allowed methods available on the ContactFormController",
            3, ContactFormController.allowedMethods.size()
    }

    void testAllowedMethodsKeys() {
        assertTrue "Expected allowed method not available!",
            ContactFormController.allowedMethods.containsKey("send")

        assertTrue "Expected allowed method not available!",
            ContactFormController.allowedMethods.containsKey("ajaxSend")
    }

    void testIndex() {
        // Run test
        controller.index()

        // Check result
        assertEquals "Unexpected redirect action returned!", "/contactForm/create",
                response.redirectedUrl
    }

    void testSend() {
        // Mock contact form
        mockDomain(ContactForm, [new ContactForm(validProperties)])

        configureValidMailTo()

        // Mock JCaptcha service
        mockJcaptchaService.demand.validateResponse() {
            def name, def sessionId, def response -> return true }
        controller.jcaptchaService = mockJcaptchaService.createMock()

        // Mock Mail service
        mockMailService.demand.sendMail() { def closure -> return null }
        controller.mailService = mockMailService.createMock()

        // Insert parameters
        controller.params.putAll(validProperties)

        // Run test
        controller.send()

        // Check result
        assertEquals "Unexpected redirect action returned!", "/contactForm/create",
            controller.modelAndView.viewName
        assertEquals "Unexpected flash message displayed!",
            "uk.co.anthonycampbell.grails.contactform.ContactForm.success",
            controller.flash.message
    }

    void testSendWithInvalidDestinationEmail() {
        // Mock contact form
        mockDomain(ContactForm, [new ContactForm(validProperties)])

        configureInvalidMailTo()

        // Run test
        controller.send()

        // Check result
        assertEquals "Unexpected redirect action returned!", "/contactForm/create",
            controller.modelAndView.viewName
        assertEquals "Unexpected flash message displayed!",
            "uk.co.anthonycampbell.grails.contactform.ContactForm.destination.address.not.found.message",
            controller.flash.message
    }

    void testSendWithEmptyDestinationEmail() {
        // Mock contact form
        mockDomain(ContactForm, [new ContactForm(validProperties)])

        configureEmptyMailTo()

        // Run test
        controller.send()

        // Check result
        assertEquals "Unexpected redirect action returned!", "/contactForm/create",
            controller.modelAndView.viewName
        assertEquals "Unexpected flash message displayed!",
            "uk.co.anthonycampbell.grails.contactform.ContactForm.destination.address.not.found.message",
            controller.flash.message
    }

    void testSendWithNullDestinationEmail() {
        // Mock contact form
        mockDomain(ContactForm, [new ContactForm(validProperties)])

        configureNullMailTo()

        // Run test
        controller.send()

        // Check result
        assertEquals "Unexpected redirect action returned!", "/contactForm/create",
            controller.modelAndView.viewName
        assertEquals "Unexpected flash message displayed!",
            "uk.co.anthonycampbell.grails.contactform.ContactForm.destination.address.not.found.message",
            controller.flash.message
    }

    void testSendWithInvalidForm() {
        // Mock contact form
        mockDomain(ContactForm, [new ContactForm(emptyProperties)])

        configureValidMailTo()

        // Insert parameters
        controller.params.putAll(emptyProperties)

        // Run test
        controller.send()

        // Check result
        assertEquals "Unexpected redirect action returned!", "/contactForm/create",
            controller.modelAndView.viewName
        assertEquals "Unexpected flash message displayed!", null,
            controller.flash.message
    }

    void testSendWithInvalidCaptcha() {
        // Mock contact form
        mockDomain(ContactForm, [new ContactForm(validProperties)])

        configureValidMailTo()

        // Mock JCaptcha service
        mockJcaptchaService.demand.validateResponse() {
            def name, def sessionId, def response -> return false }
        controller.jcaptchaService = mockJcaptchaService.createMock()

        // Insert parameters
        controller.params.putAll(validProperties)

        // Run test
        controller.send()

        // Check result
        assertEquals "Unexpected redirect action returned!", "/contactForm/create",
            controller.modelAndView.viewName
        assertEquals "Unexpected flash message displayed!", null,
            controller.flash.message
    }

    void testSendWithCaptchaFailure() {
        // Mock contact form
        mockDomain(ContactForm, [new ContactForm(validProperties)])

        configureValidMailTo()

        // Insert parameters
        controller.params.putAll(validProperties)

        // Run test
        controller.send()

        // Check result
        assertEquals "Unexpected redirect action returned!", "/contactForm/create",
            controller.modelAndView.viewName
        assertEquals "Unexpected flash message displayed!", null,
            controller.flash.message
    }

    void testSendWithMailServiceFailure() {
        // Mock contact form
        mockDomain(ContactForm, [new ContactForm(validProperties)])

        configureValidMailTo()

        // Mock JCaptcha service
        mockJcaptchaService.demand.validateResponse() {
            def name, def sessionId, def response -> return true }
        controller.jcaptchaService = mockJcaptchaService.createMock()

        mockMailService.demand.sendMail() { def closure -> throw new RuntimeException("sendMail failed, as expected") }
        controller.mailService = mockMailService.createMock()

        // Insert parameters
        controller.params.putAll(validProperties)

        // Run test
        controller.send()

        // Check result
        assertEquals "Unexpected redirect action returned!", "/contactForm/create",
            controller.modelAndView.viewName
        assertEquals "Unexpected flash message displayed!",
            "uk.co.anthonycampbell.grails.contactform.ContactForm.send.fail",
            controller.flash.message
    }

    void testAjaxSend() {
        // Mock contact form
        mockDomain(ContactForm, [new ContactForm(validProperties)])

        configureValidMailTo()

        // Mock JCaptcha service
        mockJcaptchaService.demand.validateResponse() {
            def name, def sessionId, def response -> return true }
        controller.jcaptchaService = mockJcaptchaService.createMock()

        // Mock Mail service
        mockMailService.demand.sendMail() { def closure -> return null }
        controller.mailService = mockMailService.createMock()

        // Insert parameters
        controller.params.putAll(validProperties)

        // Run test
        controller.ajaxSend()

        // Check result
        assertEquals "Unexpected redirect action returned!", "/contactForm/_create",
            controller.modelAndView.viewName
        assertEquals "Unexpected flash message displayed!",
            "uk.co.anthonycampbell.grails.contactform.ContactForm.success",
            controller.flash.message
    }

    void testAjaxSendWithInvalidDestinationEmail() {
        // Mock contact form
        mockDomain(ContactForm, [new ContactForm(validProperties)])

        configureInvalidMailTo()

        // Run test
        controller.ajaxSend()

        // Check results
        assertEquals "Unexpected redirect action returned!", "/contactForm/_create",
            controller.modelAndView.viewName
        assertEquals "Unexpected flash message displayed!",
            "uk.co.anthonycampbell.grails.contactform.ContactForm.destination.address.not.found.message",
            controller.flash.message
    }

    void testAjaxSendWithEmptyDestinationEmail() {
        // Mock contact form
        mockDomain(ContactForm, [new ContactForm(validProperties)])

        configureEmptyMailTo()

        // Run test
        controller.ajaxSend()

        // Check result
        assertEquals "Unexpected redirect action returned!", "/contactForm/_create",
            controller.modelAndView.viewName
        assertEquals "Unexpected flash message displayed!",
            "uk.co.anthonycampbell.grails.contactform.ContactForm.destination.address.not.found.message",
            controller.flash.message
    }

    void testAjaxSendWithNullDestinationEmail() {
        // Mock contact form
        mockDomain(ContactForm, [new ContactForm(validProperties)])

        configureNullMailTo()

        // Run test
        controller.ajaxSend()

        // Check result
        assertEquals "Unexpected redirect action returned!", "/contactForm/_create",
            controller.modelAndView.viewName
        assertEquals "Unexpected flash message displayed!",
            "uk.co.anthonycampbell.grails.contactform.ContactForm.destination.address.not.found.message",
            controller.flash.message
    }

    void testAjaxSendWithInvalidForm() {
        // Mock contact form
        mockDomain(ContactForm, [new ContactForm(emptyProperties)])

        configureValidMailTo()

        // Insert parameters
        controller.params.putAll(emptyProperties)

        // Run test
        controller.ajaxSend()

        // Check result
        assertEquals "Unexpected redirect action returned!", "/contactForm/_create",
            controller.modelAndView.viewName
        assertEquals "Unexpected flash message displayed!", null,
            controller.flash.message
    }

    void testAjaxSendWithInvalidCaptcha() {
        // Mock contact form
        mockDomain(ContactForm, [new ContactForm(validProperties)])

        configureValidMailTo()

        // Mock JCaptcha service
        mockJcaptchaService.demand.validateResponse() {
            def name, def sessionId, def response -> return false }
        controller.jcaptchaService = mockJcaptchaService.createMock()

        // Insert parameters
        controller.params.putAll(validProperties)

        // Run test
        controller.ajaxSend()

        // Check result
        assertEquals "Unexpected redirect action returned!", "/contactForm/_create",
            controller.modelAndView.viewName
        assertEquals "Unexpected flash message displayed!", null,
            controller.flash.message
    }

    void testAjaxSendWithCaptchaFailure() {
        // Mock contact form
        mockDomain(ContactForm, [new ContactForm(validProperties)])

        configureValidMailTo()

        // Insert parameters
        controller.params.putAll(validProperties)

        // Run test
        controller.ajaxSend()

        // Check result
        assertEquals "Unexpected redirect action returned!", "/contactForm/_create",
            controller.modelAndView.viewName
        assertEquals "Unexpected flash message displayed!", null,
            controller.flash.message
    }

    void testAjaxSendWithMailServiceFailure() {
        // Mock contact form
        mockDomain(ContactForm, [new ContactForm(validProperties)])

        configureValidMailTo()

        // Mock JCaptcha service
        mockJcaptchaService.demand.validateResponse() {
            def name, def sessionId, def response -> return true }
        controller.jcaptchaService = mockJcaptchaService.createMock()

        // Mock MailService
        mockMailService.demand.sendMail() { def closure -> throw new RuntimeException("sendMail failed, as expected") }
        controller.mailService = mockMailService.createMock()

        // Insert parameters
        controller.params.putAll(validProperties)

        // Run test
        controller.ajaxSend()

        // Check result
        assertEquals "Unexpected redirect action returned!", "/contactForm/_create",
            controller.modelAndView.viewName
        assertEquals "Unexpected flash message displayed!",
            "uk.co.anthonycampbell.grails.contactform.ContactForm.send.fail",
            controller.flash.message
    }

    void testValidate() {
        // Mock contact form
        mockDomain(ContactForm, [new ContactForm(validProperties)])

        // Insert parameters
        controller.params.putAll(validProperties)

        // Run test
        controller.validate()

        // Check result
        assertEquals "Unexpected error message displayed!", "",
            controller.response.contentAsString
    }

    void testValidateWithEmptyField() {
        def errorCode = "error.code"

        // Declare field to validate
        def emptyField = [yourFullName: ""]

        // Mock contact form
        mockDomain(ContactForm, [new ContactForm(emptyField)])

        // Insert paerror.coderameters
        controller.params.putAll(emptyField)
        
        // Mock message source
        controller.messageSource =
            [getMessage: { def fieldError, def locale -> return errorCode }]

        // Run test
        controller.validate()

        // Check result
        assertEquals "Unexpected error message displayed!", errorCode,
                controller.response.contentAsString
    }

    def configureValidMailTo() {
        controller.grailsApplication.config.mail.to = "test@contactform.com"
    }

    def configureInvalidMailTo() {
        controller.grailsApplication.config.mail.to = "invalid"
    }

    def configureEmptyMailTo() {
        controller.grailsApplication.config.mail.to = ""
    }

    def configureNullMailTo() {
        controller.grailsApplication.config.mail.to = null
    }

}