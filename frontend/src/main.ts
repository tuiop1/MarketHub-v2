import { bootstrapApplication } from '@angular/platform-browser';
import { AppComponent } from './app/app.component';
import { appConfig } from './app/app.config';
bootstrapApplication(AppComponent, appConfig).catch(() => {
  const root = document.querySelector('app-root');
  if (root) root.textContent = 'MarketHub could not start. Reload the page to try again.';
});
