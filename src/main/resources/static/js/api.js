// const Api = {

//   async _req(method, url, body) {
//   const token = Auth.getToken();
//   const headers = { 'Content-Type': 'application/json' };

//   if (token) headers['Authorization'] = `Bearer ${token}`;

//   const opts = { method, headers };

//   if (body !== undefined) {
//     opts.body = JSON.stringify(body);
//   }

//   let res, data;

//   try {
//     res = await fetch(`${CONFIG.API_BASE}${url}`, opts);

//     const text = await res.text();
//     data = text ? JSON.parse(text) : null;

//   } catch (e) {
//     throw new Error('Cannot connect to server. Is backend running?');
//   }

//   if (!res.ok) {

//     const message =
//       data?.message ||
//       data?.error ||
//       `Request failed (${res.status})`;

//     throw new Error(message);
//   }

//   return data;
//  },

//   get:    (url)       => Api._req('GET',    url),
//   post:   (url, body) => Api._req('POST',   url, body),
//   put:    (url, body) => Api._req('PUT',    url, body),
//   patch:  (url, body) => Api._req('PATCH',  url, body),
//   del:    (url)       => Api._req('DELETE', url),

//   login:    body => Api.post('/api/auth/login', body),
//   register: body => Api.post('/api/users/register', body),

//   getUserById:  id      => Api.get(`/api/users/${id}`),
//   getAllUsers:  (p = 0) => Api.get(`/api/users?page=${p}&size=10`),
//   updateUser:  (id, b) => Api.put(`/api/users/${id}`, b),
//   deleteUser:  id      => Api.del(`/api/users/${id}`),

//   addTrain:        body         => Api.post('/api/trains', body),
//   getAllTrains:     (p = 0)     => Api.get(`/api/trains?page=${p}&size=10`),
//   getTrainById:    id          => Api.get(`/api/trains/${id}`),
//   searchTrains:    (f, t, date) => Api.get(
//     `/api/trains/search?from=${encodeURIComponent(f)}&to=${encodeURIComponent(t)}&travelDate=${date}`
//   ),
//   updateTrain:       (id, body)   => Api.put(`/api/trains/${id}`, body),
//   updateTrainStatus: (id, status) => Api.patch(`/api/trains/${id}/status?status=${status}`),
//   deleteTrain:       id          => Api.del(`/api/trains/${id}`),

//   addStation:    body     => Api.post('/api/stations', body),
//   getAllStations: ()       => Api.get('/api/stations'),
//   getStationById: id      => Api.get(`/api/stations/${id}`),
//   searchStations: q       => Api.get(`/api/stations/search?q=${encodeURIComponent(q)}`),
//   updateStation: (id, b)  => Api.put(`/api/stations/${id}`, b),
//   deleteStation: id       => Api.del(`/api/stations/${id}`),

//   addSeats:          body        => Api.post('/api/seats', body),
//   getSeatsByTrain:   trainId     => Api.get(`/api/seats/train/${trainId}`),
//   getAvailableSeats: (id, date)  => Api.get(
//     `/api/seats/train/${id}/available?travelDate=${date}`
//   ),

//   getTrainSeatAvailability: (id, date) => Api.get(
//     `/api/seats/train/${id}/availability?travelDate=${date}`
//   ),
//   deleteSeat: id => Api.del(`/api/seats/${id}`),

//   bookTicket:      body          => Api.post('/api/bookings', body),
//   cancelBooking:   id            => Api.put(`/api/bookings/${id}/cancel`),
//   getBookingById:  id            => Api.get(`/api/bookings/${id}`),
//   getBookingByPnr: pnr           => Api.get(`/api/bookings/pnr/${pnr}`),
//   getUserBookings: (uid, p = 0)  => Api.get(`/api/bookings/user/${uid}?page=${p}&size=10`),
//   getAllBookings:  (p = 0, status = 'ALL') => {
//     const statusParam = status === 'ALL' ? '' : `&status=${status}`;
//     return Api.get(`/api/bookings?page=${p}&size=10${statusParam}`);
//   },

//   makePayment:         body      => Api.post('/api/payments', body),
//   processRefund:       bookingId => Api.post(`/api/payments/refund/${bookingId}`),
//   getPaymentByBooking: bookingId => Api.get(`/api/payments/booking/${bookingId}`),
// };




const Api = {

  async _req(method, url, body) {
    const token = Auth.getToken();
    const headers = { 'Content-Type': 'application/json' };

    if (token) headers['Authorization'] = `Bearer ${token}`;

    const opts = { method, headers };

    if (body !== undefined) {
      opts.body = JSON.stringify(body);
    }

    let res, data;

    try {
      res = await fetch(`${CONFIG.API_BASE}${url}`, opts);

      const text = await res.text();
      data = text ? JSON.parse(text) : null;

    } catch (e) {
      throw new Error('Cannot connect to server. Is backend running?');
    }

    if (!res.ok) {
      // If backend returned validationErrors, format them nicely
      if (data?.validationErrors && typeof data.validationErrors === 'object') {
        const errLines = Object.entries(data.validationErrors)
          .map(([field, msg]) => `• ${field}: ${msg}`)
          .join('\n');
        throw new Error(errLines);
      }

      const message =
        data?.message ||
        data?.error ||
        `Request failed (${res.status})`;

      throw new Error(message);
    }

    return data;
  },

  get:    (url)       => Api._req('GET',    url),
  post:   (url, body) => Api._req('POST',   url, body),
  put:    (url, body) => Api._req('PUT',    url, body),
  patch:  (url, body) => Api._req('PATCH',  url, body),
  del:    (url)       => Api._req('DELETE', url),

  login:    body => Api.post('/api/auth/login', body),
  register: body => Api.post('/api/users/register', body),
  changePassword: (id, body)  => Api.put(`/api/users/${id}/change-password`, body),
  sendOtp:        body        => Api.post('/api/users/forgot-password/send-otp', body),
  resetPassword:  body        => Api.post('/api/users/forgot-password/reset', body),

  getUserById:  id      => Api.get(`/api/users/${id}`),
  getAllUsers:  (p = 0) => Api.get(`/api/users?page=${p}&size=10`),
  updateUser:  (id, b) => Api.put(`/api/users/${id}`, b),
  deleteUser:  id      => Api.del(`/api/users/${id}`),

  addTrain:        body         => Api.post('/api/trains', body),
  getAllTrains:     (p = 0)     => Api.get(`/api/trains?page=${p}&size=10`),
  getTrainById:    id          => Api.get(`/api/trains/${id}`),
  searchTrains:    (f, t, date) => Api.get(
    `/api/trains/search?from=${encodeURIComponent(f)}&to=${encodeURIComponent(t)}&travelDate=${date}`
  ),
  updateTrain:       (id, body)   => Api.put(`/api/trains/${id}`, body),
  updateTrainStatus: (id, status) => Api.patch(`/api/trains/${id}/status?status=${status}`),
  deleteTrain:       id          => Api.del(`/api/trains/${id}`),

  addStation:    body     => Api.post('/api/stations', body),
  getAllStations: ()       => Api.get('/api/stations'),
  getStationById: id      => Api.get(`/api/stations/${id}`),
  searchStations: q       => Api.get(`/api/stations/search?q=${encodeURIComponent(q)}`),
  updateStation: (id, b)  => Api.put(`/api/stations/${id}`, b),
  deleteStation: id       => Api.del(`/api/stations/${id}`),

  addSeats:          body        => Api.post('/api/seats', body),
  getSeatsByTrain:   trainId     => Api.get(`/api/seats/train/${trainId}`),
  getAvailableSeats: (id, date)  => Api.get(
    `/api/seats/train/${id}/available?travelDate=${date}`
  ),

  getTrainSeatAvailability: (id, date) => Api.get(
    `/api/seats/train/${id}/availability?travelDate=${date}`
  ),
  deleteSeat: id => Api.del(`/api/seats/${id}`),

  bookTicket:      body          => Api.post('/api/bookings', body),
  cancelBooking:   id            => Api.put(`/api/bookings/${id}/cancel`),
  getBookingById:  id            => Api.get(`/api/bookings/${id}`),
  getBookingByPnr: pnr           => Api.get(`/api/bookings/pnr/${pnr}`),
  getUserBookings: (uid, p = 0)  => Api.get(`/api/bookings/user/${uid}?page=${p}&size=10`),
  getAllBookings:  (p = 0, status = 'ALL') => {
    const statusParam = status === 'ALL' ? '' : `&status=${status}`;
    return Api.get(`/api/bookings?page=${p}&size=10${statusParam}`);
  },

  makePayment:         body      => Api.post('/api/payments', body),
  processRefund:       bookingId => Api.post(`/api/payments/refund/${bookingId}`),
  getPaymentByBooking: bookingId => Api.get(`/api/payments/booking/${bookingId}`),
};