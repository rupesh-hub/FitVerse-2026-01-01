import {inject, Injectable} from '@angular/core';
import {HttpClient} from '@angular/common/http';
import {Observable} from 'rxjs';
import {API_CONSTANTS} from '../constants';

@Injectable({
  providedIn: 'root'
})
export class WelcomeService {

  private http:HttpClient = inject(HttpClient);

  public welcome = ():Observable<any> =>{
    return this.http.get<any>(`${API_CONSTANTS.ENDPOINTS.MESSAGE}`);
  }

}
