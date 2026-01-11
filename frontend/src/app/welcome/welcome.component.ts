import {Component, inject, OnInit} from '@angular/core';
import {CommonModule} from '@angular/common';
import {WelcomeService} from './welcome.service';

@Component({
  selector: 'fit-verse-welcome',
  imports: [CommonModule],
  standalone: true,
  template: `
    <p class="font-serif font-bold text-indigo-600 flex justify-center items-center text-xs">
      {{ message }}
    </p>
  `,
  styles: ``
})
export class WelcomeComponent implements OnInit {

  private welcomeService: WelcomeService = inject(WelcomeService);
  protected message: any;

  public ngOnInit(): void {
    this.welcomeService.welcome()
      .subscribe({
        next: (response: any) => this.message = response.content,
        error: (err: any) => console.log(err),
        complete: () => console.log(' complete')
      })
  }

}
