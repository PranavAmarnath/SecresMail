# SecresMail
A mail client made in Java.

How the application looks:
<p align="left">
    <img src="https://user-images.githubusercontent.com/64337291/114258589-65551000-997c-11eb-8b12-94ce5ece8f7f.png" width=400 />
    <img src="https://user-images.githubusercontent.com/64337291/114258626-c0870280-997c-11eb-95b6-cf23411dcf3f.png" width=700 />
    <img src="https://user-images.githubusercontent.com/64337291/114258718-7e11f580-997d-11eb-9bf2-ca66be2e8a0e.png" width=700 />
    <img src="https://user-images.githubusercontent.com/64337291/114258991-9125c500-997f-11eb-997d-ad9b44ff172c.png" width=700 />
</p>

Features:
* Secure login
* Reading all emails from an email account in table format with find/search support using IMAP with SSL
* Sending emails using SMTP (smtp.gmail.com) with SSL
* Marking as unread/read with instantaneous sync to server
* Sorting by various columns
* Synchronous and immediate updates from server to client (table)
    * Email addition/removal
    * 'Mark as read' on a different client or on the server
* Viewing any included attachment filenames in list format

Libraries Used:
* JDK (with Java Swing - main GUI)
* Maven for dependency management and build automation (Shade Plugin)
* OpenJFX (JavaFX `WebView` - for rendering of HTML/CSS/JS content)
* SwingX (`JXLoginPane` for secure login)
* FlatLaf (Core, Extras, SwingX - for modern look and feel)
* Jakarta Mail (formerly JavaMail - core backend of reading/writing emails from server)
