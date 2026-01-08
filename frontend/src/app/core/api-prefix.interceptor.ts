import {HttpInterceptorFn} from '@angular/common/http';
import {environment} from '../../environments/environment-default';

export const apiPrefixInterceptor: HttpInterceptorFn = (req, next) => {
  if (req.url.startsWith('http')) return next(req);
  return next(
    req.clone({url: `${environment.API_URL}${req.url}`})
  );
};
