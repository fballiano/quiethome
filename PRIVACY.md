# Privacy

QuietHome does not collect data. It does not keep, send or share data about you, your TV or what you watch. It has no accounts, no analytics and no ads.

## Your settings

Your settings stay on the TV, in the private app storage of QuietHome: the look of Home, the apps on Home, the input names and the button mapping. Other apps cannot read this storage. Backups are off, so your settings do not go to a cloud backup. When you uninstall QuietHome, Android deletes your settings.

## The QuietHome service

The QuietHome service is an accessibility service. It is off until you turn it on. Before you turn it on, QuietHome shows what the service does, and you select **Agree and open settings**.

The service does not collect data. It does two checks on the TV, at the moment that something occurs, and then it forgets:

- When you press a button on the remote, the service checks if the button is Home or a button that you mapped. If it is, the service does the job of that button. All other key presses go to the TV as usual.
- When a different app comes to the front, the service checks if it is the TV's own home screen. If it is, the service opens QuietHome.

The service does not keep the key presses or the app names, and it does not send them. It cannot read the screen.

## Network

The Google Play version does not have the permission to use the network. Google Play updates it.

The GitHub version uses the network only when you select Settings → Update QuietHome. It then reads the latest release from GitHub and downloads it. GitHub receives the request, as with each download. The [GitHub Privacy Statement](https://docs.github.com/site-policy/privacy-policies/github-general-privacy-statement) tells what GitHub does with it. QuietHome sends nothing about you or your TV.

## Contact

For questions about privacy, open an issue at <https://github.com/fballiano/quiethome/issues>. To report a security problem, follow [SECURITY.md](SECURITY.md).
