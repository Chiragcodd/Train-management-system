if (!Auth.requireAuth()) throw '';
renderNavUser();

qs('#checkBtn').addEventListener('click', checkPnr);
qs('#pnrInput').addEventListener('keydown', e => {
  if (e.key === 'Enter') checkPnr();
});

async function checkPnr() {
  const pnr = qs('#pnrInput').value.trim().toUpperCase();
  if (!pnr) { Toast.warning('Enter a PNR number'); return; }

  const btn = qs('#checkBtn');
  btnLoad(btn, true, 'Checking...');
  hide(qs('#pnrResult'));
  hide(qs('#notFound'));

  try {
    const b = await Api.getBookingByPnr(pnr);

    setText('pPnr',    b.pnrNumber);
    setText('pBooked', `Booked: ${fmtDateTime(b.bookingDate)}`);
    setText('pFrom',   b.fromStation);
    setText('pTo',     b.toStation);
    setText('pTrain',  `${b.trainName} (${b.trainNumber})`);
    setText('pDate',   fmtDate(b.travelDate));
    setText('pCoach',  b.coachType);
    setText('pAmount', fmtMoney(b.totalAmount));

    // Passenger count — CNF + WL breakdown
    const cnf = b.confirmedCount ?? 0;
    const wl  = b.waitlistedCount ?? 0;
    const paxCountEl = qs('#pPaxCount');
    if (paxCountEl) {
      if (wl > 0 && cnf > 0) {
        paxCountEl.innerHTML =
          `${b.passengers.length} &nbsp;`
          + `<span class="badge badge-green" style="font-size:.72rem;">✅ ${cnf} CNF</span> `
          + `<span class="badge badge-orange" style="font-size:.72rem;">⏳ ${wl} WL</span>`;
      } else if (wl > 0) {
        paxCountEl.innerHTML =
          `${b.passengers.length} &nbsp;`
          + `<span class="badge badge-orange" style="font-size:.72rem;">⏳ All Waitlisted</span>`;
      } else {
        paxCountEl.textContent = b.passengers.length;
      }
    }

    // Fare info
    const fareEl = qs('#pFare');
    if (fareEl) {
      fareEl.textContent = b.fareBreakdown
        ? b.fareBreakdown
        : b.farePerPassenger
          ? `${fmtMoney(b.farePerPassenger)}/passenger · ${(b.journeyDistanceKm || 0).toFixed(1)} km`
          : '—';
    }

    statusBadge(b.status, qs('#pStatus'));

    // Waitlist notice (only if ALL passengers are WL)
    const wlEl = qs('#pWlNotice');
    if (wlEl) {
      if (b.status === 'WAITLISTED' && wl > 0) {
        const pos = b.waitlistNumber ? `WL/${b.waitlistNumber}` : 'Waitlisted';
        wlEl.textContent = b.pnrNumber
          ? `⌛ ${pos} — Payment done. Seat will be confirmed when someone cancels.`
          : `⌛ ${pos} — Pending payment.`;
        show(wlEl);
      } else if (wl > 0 && cnf > 0) {
        // Mixed booking — kuch CNF kuch WL
        wlEl.innerHTML =
          `⚠️ Mixed booking: <strong>${cnf} passenger(s) confirmed</strong>, `
          + `<strong>${wl} passenger(s) on waitlist</strong>. `
          + `Waitlisted passengers will get seats when others cancel.`;
        wlEl.className = 'alert alert-warning';
        show(wlEl);
      } else {
        hide(wlEl);
      }
    }

    // ── Passenger Table ──────────────────────────────────────────
    const tbody = qs('#paxTableBody');
    if (tbody) {
      tbody.innerHTML = '';
      if ((b.passengers || []).length) {
        b.passengers.forEach(p => {
          const row = cloneTpl('tpl-pax-row');

          row.querySelector('[data-f="name"]').textContent   = p.name;
          row.querySelector('[data-f="age"]').textContent    = p.age;
          row.querySelector('[data-f="gender"]').textContent = p.gender;
          row.querySelector('[data-f="fare"]').textContent   = p.fare ? fmtMoney(p.fare) : '—';

          // Per-passenger CNF/WL status — main fix
          const seatCell   = row.querySelector('[data-f="seat"]');
          const statusCell = row.querySelector('[data-f="pax-status"]');

          if (p.passengerStatus === 'CONFIRMED') {
            if (seatCell)
              seatCell.textContent = p.seatNumber && p.seatNumber !== 0
                ? p.seatNumber : '—';
            if (statusCell)
              statusCell.innerHTML = '<span class="badge badge-green">✅ CNF</span>';
          } else {
            // WAITLISTED
            if (seatCell)
              seatCell.textContent = '—';
            if (statusCell)
              statusCell.innerHTML =
                `<span class="badge badge-orange">⏳ WL/${p.waitlistPosition ?? '?'}</span>`;
          }

          tbody.appendChild(row);
        });
        show(qs('#paxSection'));
      } else {
        hide(qs('#paxSection'));
      }
    }

    // Refund notice
    const refundEl = qs('#pRefundNotice');
    if (b.status === 'CANCELLED' && parseFloat(b.refundAmount || 0) > 0) {
      refundEl.textContent = `💰 Refund Amount: ${fmtMoney(b.refundAmount)}`;
      show(refundEl);
    } else {
      hide(refundEl);
    }

    show(qs('#pnrResult'));

  } catch (err) {
    setText('notFoundMsg', err.message);
    show(qs('#notFound'));
  } finally {
    btnLoad(btn, false);
  }
}