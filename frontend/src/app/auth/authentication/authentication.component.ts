import { Component } from '@angular/core';
import {CommonModule} from '@angular/common';
import {RouterModule} from '@angular/router';
import {WelcomeComponent} from '../../welcome/welcome.component';

@Component({
  selector: 'fit-verse-authentication',
  imports: [CommonModule, RouterModule, WelcomeComponent],
  standalone: true,
  templateUrl: './authentication.component.html'
})
export class AuthenticationComponent {

}
