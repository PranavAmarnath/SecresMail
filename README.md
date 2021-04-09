# SecresMail
A mail client made in Java.

How the application looks (Blurred for confidentiality):
<p align="left">
    <img src="https://user-images.githubusercontent.com/64337291/114111087-ef727b00-988d-11eb-8376-7d9b1706a6c3.png" width=700 />
    <img src="https://user-images.githubusercontent.com/64337291/114112975-6f9adf80-9892-11eb-81a5-ae32ccf9e1e9.png" width=700 />
</p>

Features:
* Secure login
* Reading all emails from an email account in table format with find/search support
* Marking as unread/read with instantaneous sync to server
* Sorting by various columns
* Synchronous and immediate updates from server to client (table) for email addition and/or removal
* Viewing any included attachment filenames in list format (support for opening actual attachments predicted for future release)

Libraries Used:
* JDK (with Java Swing - main GUI)
* Maven for dependency management and build automation (Shade Plugin)
* OpenJFX (JavaFX - for rendering of HTML/CSS/JS content)
* SwingX (`JXLoginPane` for secure login)
* FlatLaf (Core, Extras, SwingX - for modern look and feel)
* Jakarta Mail (formerly JavaMail - core backend of reading/writing emails from server)
